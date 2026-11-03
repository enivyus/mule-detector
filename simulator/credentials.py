"""HTTP Basic headers for the services. Passwords come from the same env vars the services read,
with the same local dev defaults (see each service's application.yml). Standard library only."""
from __future__ import annotations

import base64
import os

DEV_PASSWORDS = {"channel": "channel-dev", "investigator": "investigator-dev"}


def basic_auth(user: str) -> dict[str, str]:
    password = os.environ.get(f"{user.upper()}_PASSWORD", DEV_PASSWORDS[user])
    return {"Authorization": "Basic " + base64.b64encode(f"{user}:{password}".encode()).decode()}
