from tests.test_rate_limit import build_test_client


def test_429_response_includes_rate_limit_headers(monkeypatch, fake_redis):
    client = build_test_client(monkeypatch, fake_redis)

    responses = [client.get("/metered") for _ in range(6)]
    limited = responses[-1]

    assert limited.status_code == 429
    assert limited.headers["X-RateLimit-Limit"] == "5"
    assert limited.headers["X-RateLimit-Remaining"] == "0"
