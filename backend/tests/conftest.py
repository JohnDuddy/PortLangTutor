import pytest

from app.deps.auth import CurrentUser


class FakeRedis:
    def __init__(self):
        self.values = {}
        self.expirations = {}

    async def get(self, key):
        return self.values.get(key)

    async def set(self, key, value, ex=None):
        self.values[key] = str(value)
        if ex:
            self.expirations[key] = ex

    async def incrby(self, key, amount=1):
        next_value = int(self.values.get(key, 0)) + amount
        self.values[key] = str(next_value)
        return next_value

    async def expire(self, key, seconds):
        self.expirations[key] = seconds

    async def ping(self):
        return True


@pytest.fixture
def current_user():
    return CurrentUser(user_id="user-123", email="test@example.com", tier_name="free")


@pytest.fixture
def fake_redis():
    return FakeRedis()
