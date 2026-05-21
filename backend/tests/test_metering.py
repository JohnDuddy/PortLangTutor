import pytest
from fastapi import HTTPException

from app.middleware import metering


@pytest.fixture(autouse=True)
def patch_store(monkeypatch, fake_redis):
    async def fake_store():
        return fake_redis

    monkeypatch.setattr(metering, "store", fake_store)
    return fake_redis


@pytest.mark.asyncio
async def test_check_quota_allows_when_under_limit():
    ok, reason = await metering.check_quota("user-123", "free", "coach", units=1)

    assert ok is True
    assert reason == ""


@pytest.mark.asyncio
async def test_check_quota_denies_when_daily_limit_reached(fake_redis):
    await fake_redis.set(f"usage:user-123:coach:day:{metering._day_key()}", 10)

    ok, reason = await metering.check_quota("user-123", "free", "coach", units=1)

    assert ok is False
    assert "Daily coach limit reached" in reason


@pytest.mark.asyncio
async def test_check_quota_denies_when_monthly_limit_reached(fake_redis):
    await fake_redis.set(f"usage:user-123:coach:month:{metering._month_key()}", 200)

    ok, reason = await metering.check_quota("user-123", "free", "coach", units=1)

    assert ok is False
    assert "Monthly coach limit reached" in reason


@pytest.mark.asyncio
async def test_charge_increments_daily_and_monthly_counters(fake_redis):
    await metering.charge("user-123", "pron", units=3)

    assert await fake_redis.get(f"usage:user-123:pron:day:{metering._day_key()}") == "3"
    assert await fake_redis.get(f"usage:user-123:pron:month:{metering._month_key()}") == "3"


@pytest.mark.asyncio
async def test_enforce_raises_402_when_over_quota(fake_redis):
    await fake_redis.set(f"usage:user-123:coach:day:{metering._day_key()}", 10)

    with pytest.raises(HTTPException) as exc:
        await metering.enforce("user-123", "free", "coach", units=1)

    assert exc.value.status_code == 402


@pytest.mark.asyncio
async def test_get_usage_returns_current_structure(fake_redis):
    await fake_redis.set(f"usage:user-123:coach:day:{metering._day_key()}", 3)
    await fake_redis.set(f"usage:user-123:pron:month:{metering._month_key()}", 12)

    usage = await metering.get_usage("user-123")

    assert usage == {
        "coach": {"day": 3, "month": 0},
        "pron": {"day": 0, "month": 12},
    }
