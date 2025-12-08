"""
Recommendation API Routes
"""
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field
from typing import Dict, Any, Optional
import logging
import json
import time

from services.recommendation_service import RecommendationService

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/recommendation", tags=["recommendation"])

# Initialize service
recommendation_service = RecommendationService()


# Request/Response Models
class CodeChange(BaseModel):
    changed_files_count: int = Field(ge=0, description="Number of changed files")
    changed_lines_count: int = Field(ge=0, description="Number of changed lines")
    code_complexity_delta: float = Field(default=0.0, description="Complexity change")
    test_coverage_delta: float = Field(default=0.0, description="Coverage change")
    changed_modules: list = Field(default=[], description="List of changed modules")
    change_type: str = Field(default="feature", description="Type of change")


class Historical(BaseModel):
    recent_pass_rate: float = Field(default=0.95, ge=0.0, le=1.0)
    avg_execution_time: float = Field(default=30.0, ge=0.0)
    recent_defect_count: int = Field(default=0, ge=0)
    failure_frequency: float = Field(default=0.05, ge=0.0, le=1.0)


class Business(BaseModel):
    module_importance: float = Field(default=0.5, ge=0.0, le=1.0)
    business_priority: str = Field(default="P2", pattern="^P[0-3]$")
    release_urgency: str = Field(default="normal")


class Environment(BaseModel):
    available_resources: int = Field(default=5, ge=1)
    queue_length: int = Field(default=0, ge=0)
    env_stability_score: float = Field(default=0.95, ge=0.0, le=1.0)
    current_load: float = Field(default=0.5, ge=0.0, le=1.0)


class TaskContext(BaseModel):
    code_change: CodeChange
    historical: Historical = Field(default_factory=Historical)
    business: Business = Field(default_factory=Business)
    environment: Environment = Field(default_factory=Environment)


class RecommendationRequest(BaseModel):
    task_id: str = Field(..., description="Task ID")
    environment_id: Optional[str] = Field(None, description="Test environment ID")
    version_id: Optional[str] = Field(None, description="Test version ID")
    context: TaskContext


class RecommendationResponse(BaseModel):
    task_id: str
    recommendation: Dict[str, Any]
    risk_assessment: Dict[str, Any]
    environment_recommendations: list
    timestamp: str


@router.post("/strategy", response_model=RecommendationResponse)
async def recommend_strategy(request: RecommendationRequest):
    """
    Recommend test strategy for a task

    Returns recommendation with test scope, environment, priority, etc.
    """
    start_time = time.time()
    try:
        # Log request
        logger.info(f"[REQUEST] POST /recommendation/strategy\n{json.dumps(request.dict(), ensure_ascii=False, indent=2)}")

        result = recommendation_service.recommend_strategy(request.dict())

        # Log response
        elapsed_time = time.time() - start_time
        logger.info(f"[RESPONSE] POST /recommendation/strategy - Status: SUCCESS, Time: {elapsed_time:.2f}s\n{json.dumps(result, ensure_ascii=False, indent=2, default=str)}")

        return result

    except Exception as e:
        elapsed_time = time.time() - start_time
        logger.error(f"[RESPONSE] POST /recommendation/strategy - Status: FAILED, Time: {elapsed_time:.2f}s, Error: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/strategy/explain")
async def explain_recommendation(
    recommendation: Dict[str, Any],
    context: TaskContext
):
    """
    Get detailed explanation for a recommendation
    """
    start_time = time.time()
    try:
        # Log request
        logger.info(f"[REQUEST] POST /recommendation/strategy/explain\n{json.dumps({'recommendation': recommendation, 'context': context.dict()}, ensure_ascii=False, indent=2)}")

        explanation = recommendation_service.get_recommendation_explanation(
            recommendation=recommendation,
            context=context.dict()
        )

        result = {
            'success': True,
            'explanation': explanation
        }

        # Log response
        elapsed_time = time.time() - start_time
        logger.info(f"[RESPONSE] POST /recommendation/strategy/explain - Status: SUCCESS, Time: {elapsed_time:.2f}s\n{json.dumps(result, ensure_ascii=False, indent=2, default=str)}")

        return result

    except Exception as e:
        elapsed_time = time.time() - start_time
        logger.error(f"[RESPONSE] POST /recommendation/strategy/explain - Status: FAILED, Time: {elapsed_time:.2f}s, Error: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/health")
async def health_check():
    """Health check endpoint"""
    start_time = time.time()
    try:
        logger.info(f"[REQUEST] GET /recommendation/health")

        result = {
        'status': 'UP',
        'service': 'recommendation',
        'models_loaded': True
    }

        elapsed_time = time.time() - start_time
        logger.info(f"[RESPONSE] GET /recommendation/health - Status: SUCCESS, Time: {elapsed_time:.2f}s\n{json.dumps(result, ensure_ascii=False, indent=2)}")

        return result

    except Exception as e:
        elapsed_time = time.time() - start_time
        logger.error(f"[RESPONSE] GET /recommendation/health - Status: FAILED, Time: {elapsed_time:.2f}s, Error: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))
