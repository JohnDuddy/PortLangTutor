from fastapi import FastAPI
from fastapi.testclient import TestClient

from app.middleware import rate_limit


def build_test_client(monkeypatch, fake_redis):
    async def fake_store():
        return fake_redis

    monkeypatch.setattr(rate_limit, "store", fake_store)
    monkeypatch.setattr(rate_limit, "verify_jwt", lambda token: {"sub": "user-123"})
    monkeypatch.setattr(rate_limit, "get_user_tier", lambda user_id: "pro")

    app = FastAPI()
    app.add_middleware(rate_limit.RateLimitMiddleware)

    @app.get("/health")
    async def health():
        return {"ok": True}

    @app.get("/metered")
    async def metered():
        return {"ok": True}

    return TestClient(app)


def test_exempt_paths_bypass_rate_limiting(monkeypatch, fake_redis):
    client = build_test_client(monkeypatch, fake_redis)

    response = client.get("/health")

    assert response.status_code == 200
    assert "X-RateLimit-Limit" not in response.headers


def test_authenticated_user_gets_tier_based_limit(monkeypatch, fake_redis):
    client = build_test_client(monkeypatch, fake_redis)

    response = client.get("/metered", headers={"Authorization": "Bearer good-token"})

    assert response.status_code == 200
    assert response.headers["X-RateLimit-Limit"] == "60"


def test_anonymous_user_is_limited_to_five_requests_per_minute(monkeypatch, fake_redis):
    client = build_test_client(monkeypatch, fake_redis)

    responses = [client.get("/metered") for _ in range(6)]

    assert [response.status_code for response in responses[:5]] == [200, 200, 200, 200, 200]
    assert responses[5].status_code == 429
    assert responses[5].headers["Retry-After"]
