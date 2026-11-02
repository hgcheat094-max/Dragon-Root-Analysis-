from contextlib import asynccontextmanager

from fastapi import FastAPI

from .config import settings
from .db import Base, engine
from .errors import install_handlers
from .ratelimit import RateLimitMiddleware
from .routers import admin, public


@asynccontextmanager
async def lifespan(_: FastAPI):
    # Dev/test convenience. Production schemas are managed with migrations/*.sql.
    Base.metadata.create_all(engine)
    yield


app = FastAPI(
    title="Dragon Root API",
    version="1.0.0",
    description=(
        "Read-only evidence database for Vivo/iQOO firmware, kernel profiles and compatibility research. "
        "The API never serves exploit code. Compatibility results require exact device/build/kernel evidence."
    ),
    lifespan=lifespan,
)
app.add_middleware(RateLimitMiddleware, per_minute=settings.rate_limit_per_minute)
install_handlers(app)
app.include_router(public.router, tags=["public"])
app.include_router(admin.router)


@app.get("/healthz", tags=["meta"])
def healthz():
    return {"status": "ok"}
