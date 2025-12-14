"""
Test Case Generation API Routes
"""
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field
from typing import Dict, Any, List, Optional
import logging
import json
import time

from services.testcase_service import (
    TestCaseGenerationService,
    TestCaseOptimizationService
)

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/testcase", tags=["testcase"])

# Initialize services
generation_service = TestCaseGenerationService()
optimization_service = TestCaseOptimizationService()


# Request/Response Models
class OptimizationConfig(BaseModel):
    deduplicate: bool = Field(default=True)
    prioritize: bool = Field(default=True)
    min_priority: Optional[str] = Field(default=None, pattern="^P[0-3]$")
    max_cases: Optional[int] = Field(default=None, ge=1)


class GenerationRequest(BaseModel):
    requirement_text: str = Field(..., min_length=10, description="Requirement document")
    module: str = Field(default="unknown", description="Module name")
    num_cases: int = Field(default=5, ge=1, le=50, description="Number of cases to generate")
    include_edge_cases: bool = Field(default=True)
    optimization: Optional[OptimizationConfig] = Field(default=None)


class BatchGenerationRequest(BaseModel):
    requirements: List[GenerationRequest] = Field(..., min_items=1, max_items=20)


class FeedbackRequest(BaseModel):
    request_id: str = Field(..., description="Generation request ID")
    rating: int = Field(..., ge=1, le=5, description="User rating (1-5)")
    comments: Optional[str] = Field(default=None)
    accepted_cases: List[str] = Field(default=[], description="List of accepted case names")
    rejected_cases: List[str] = Field(default=[], description="List of rejected case names")


class DeduplicationRequest(BaseModel):
    testcases: List[Dict[str, Any]] = Field(..., min_items=1)
    threshold: float = Field(default=0.85, ge=0.0, le=1.0)


class PrioritizationRequest(BaseModel):
    testcases: List[Dict[str, Any]] = Field(..., min_items=1)
    custom_weights: Optional[Dict[str, float]] = Field(default=None)


@router.post("/generate")
async def generate_testcases(request: GenerationRequest):
    """
    Generate test cases from requirement document

    Uses RAG approach with LLM + historical cases + company standards
    """
    start_time = time.time()
    try:
        # Log request
        logger.info(f"[REQUEST] POST /testcase/generate\n{json.dumps(request.dict(), ensure_ascii=False, indent=2)}")

        result = generation_service.generate_testcases(request.dict())

        # Log response
        elapsed_time = time.time() - start_time
        logger.info(f"[RESPONSE] POST /testcase/generate - Status: SUCCESS, Time: {elapsed_time:.2f}s\n{json.dumps(result, ensure_ascii=False, indent=2, default=str)}")

        return result

    except Exception as e:
        elapsed_time = time.time() - start_time
        logger.error(f"[RESPONSE] POST /testcase/generate - Status: FAILED, Time: {elapsed_time:.2f}s, Error: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/generate/batch")
async def batch_generate(request: BatchGenerationRequest):
    """
    Batch generate test cases for multiple requirements
    """
    start_time = time.time()
    try:
        # Log request
        logger.info(f"[REQUEST] POST /testcase/generate/batch\n{json.dumps(request.dict(), ensure_ascii=False, indent=2)}")

        result = generation_service.batch_generate(request.dict())

        # Log response
        elapsed_time = time.time() - start_time
        logger.info(f"[RESPONSE] POST /testcase/generate/batch - Status: SUCCESS, Time: {elapsed_time:.2f}s\n{json.dumps(result, ensure_ascii=False, indent=2, default=str)}")

        return result

    except Exception as e:
        elapsed_time = time.time() - start_time
        logger.error(f"[RESPONSE] POST /testcase/generate/batch - Status: FAILED, Time: {elapsed_time:.2f}s, Error: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/feedback")
async def submit_feedback(request: FeedbackRequest):
    """
    Submit user feedback for generated test cases

    Helps improve model through reinforcement learning
    """
    start_time = time.time()
    try:
        # Log request
        logger.info(f"[REQUEST] POST /testcase/feedback\n{json.dumps(request.dict(), ensure_ascii=False, indent=2)}")

        result = generation_service.update_user_feedback(
            request_id=request.request_id,
            feedback=request.dict()
        )

        # Log response
        elapsed_time = time.time() - start_time
        logger.info(f"[RESPONSE] POST /testcase/feedback - Status: SUCCESS, Time: {elapsed_time:.2f}s\n{json.dumps(result, ensure_ascii=False, indent=2, default=str)}")

        return result

    except Exception as e:
        elapsed_time = time.time() - start_time
        logger.error(f"[RESPONSE] POST /testcase/feedback - Status: FAILED, Time: {elapsed_time:.2f}s, Error: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/optimize/deduplicate")
async def deduplicate_testcases(request: DeduplicationRequest):
    """
    Deduplicate test cases using semantic similarity
    """
    start_time = time.time()
    try:
        # Log request
        logger.info(f"[REQUEST] POST /testcase/optimize/deduplicate\n{json.dumps(request.dict(), ensure_ascii=False, indent=2)}")

        result = optimization_service.deduplicate(
            testcases=request.testcases,
            threshold=request.threshold
        )

        # Log response
        elapsed_time = time.time() - start_time
        logger.info(f"[RESPONSE] POST /testcase/optimize/deduplicate - Status: SUCCESS, Time: {elapsed_time:.2f}s\n{json.dumps(result, ensure_ascii=False, indent=2, default=str)}")

        return result

    except Exception as e:
        elapsed_time = time.time() - start_time
        logger.error(f"[RESPONSE] POST /testcase/optimize/deduplicate - Status: FAILED, Time: {elapsed_time:.2f}s, Error: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/optimize/prioritize")
async def prioritize_testcases(request: PrioritizationRequest):
    """
    Prioritize test cases using multi-factor scoring
    """
    start_time = time.time()
    try:
        # Log request
        logger.info(f"[REQUEST] POST /testcase/optimize/prioritize\n{json.dumps(request.dict(), ensure_ascii=False, indent=2)}")

        result = optimization_service.prioritize(
            testcases=request.testcases,
            custom_weights=request.custom_weights
        )

        # Log response
        elapsed_time = time.time() - start_time
        logger.info(f"[RESPONSE] POST /testcase/optimize/prioritize - Status: SUCCESS, Time: {elapsed_time:.2f}s\n{json.dumps(result, ensure_ascii=False, indent=2, default=str)}")

        return result

    except Exception as e:
        elapsed_time = time.time() - start_time
        logger.error(f"[RESPONSE] POST /testcase/optimize/prioritize - Status: FAILED, Time: {elapsed_time:.2f}s, Error: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/analyze/quality")
async def analyze_quality(testcases: List[Dict[str, Any]]):
    """
    Analyze quality metrics of test cases
    """
    start_time = time.time()
    try:
        # Log request
        logger.info(f"[REQUEST] POST /testcase/analyze/quality\n{json.dumps({'testcases': testcases}, ensure_ascii=False, indent=2)}")

        result = optimization_service.analyze_quality(testcases)

        # Log response
        elapsed_time = time.time() - start_time
        logger.info(f"[RESPONSE] POST /testcase/analyze/quality - Status: SUCCESS, Time: {elapsed_time:.2f}s\n{json.dumps(result, ensure_ascii=False, indent=2, default=str)}")

        return result

    except Exception as e:
        elapsed_time = time.time() - start_time
        logger.error(f"[RESPONSE] POST /testcase/analyze/quality - Status: FAILED, Time: {elapsed_time:.2f}s, Error: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/history")
async def get_generation_history(
    limit: int = 50,
    offset: int = 0,
    module: Optional[str] = None
):
    """
    Get test case generation history from vector database

    Args:
        limit: Number of records to return (default: 50)
        offset: Number of records to skip (default: 0)
        module: Filter by module name (optional)
    """
    start_time = time.time()
    try:
        logger.info(f"[REQUEST] GET /testcase/history?limit={limit}&offset={offset}&module={module}")

        result = generation_service.get_generation_history(
            limit=limit,
            offset=offset,
            module=module
        )

        elapsed_time = time.time() - start_time
        logger.info(f"[RESPONSE] GET /testcase/history - Status: SUCCESS, Time: {elapsed_time:.2f}s\n{json.dumps(result, ensure_ascii=False, indent=2, default=str)}")

        return result

    except Exception as e:
        elapsed_time = time.time() - start_time
        logger.error(f"[RESPONSE] GET /testcase/history - Status: FAILED, Time: {elapsed_time:.2f}s, Error: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/health")
async def health_check():
    """Health check endpoint"""
    start_time = time.time()
    try:
        logger.info(f"[REQUEST] GET /testcase/health")

        result = {
        'status': 'UP',
        'service': 'testcase-generation',
        'llm_available': True
    }

        elapsed_time = time.time() - start_time
        logger.info(f"[RESPONSE] GET /testcase/health - Status: SUCCESS, Time: {elapsed_time:.2f}s\n{json.dumps(result, ensure_ascii=False, indent=2)}")

        return result

    except Exception as e:
        elapsed_time = time.time() - start_time
        logger.error(f"[RESPONSE] GET /testcase/health - Status: FAILED, Time: {elapsed_time:.2f}s, Error: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))
