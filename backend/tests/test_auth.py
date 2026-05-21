import pytest
from fastapi import HTTPException

from app.deps import auth


@pytest.mark.asyncio
async def test_missing_authorization_header_returns_401(monkeypatch):
    monkeypatch.setattr(auth.settings, "DEV_AUTH_BYPASS", False)

    with pytest.raises(HTTPException) as exc:
        await auth.current_user(None)

    assert exc.value.status_code == 401


@pytest.mark.asyncio
async def test_invalid_jwt_returns_401(monkeypatch):
    monkeypatch.setattr(auth, "verify_jwt", lambda token: None)

    with pytest.raises(HTTPException) as exc:
        await auth.current_user("Bearer bad-token")

    assert exc.value.status_code == 401


@pytest.mark.asyncio
async def test_valid_jwt_extracts_user_id_and_tier(monkeypatch):
    monkeypatch.setattr(
        auth,
        "verify_jwt",
        lambda token: {"sub": "user-123", "email": "test@example.com"},
    )
    monkeypatch.setattr(auth, "get_user_tier", lambda user_id: "pro")

    user = await auth.current_user("Bearer good-token")

    assert user.user_id == "user-123"
    assert user.email == "test@example.com"
    assert user.tier_name == "pro"


@pytest.mark.asyncio
async def test_dev_auth_bypass_returns_local_user(monkeypatch):
    monkeypatch.setattr(auth.settings, "DEV_AUTH_BYPASS", True)

    user = await auth.current_user(None)

    assert user.user_id == "local-dev"
    assert user.tier_name == "pro_plus"
