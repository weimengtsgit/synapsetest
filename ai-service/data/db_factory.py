"""
Database Client

MySQL client for structured data storage
"""
from typing import Optional
import logging

from data.mysql_client import mysql_client

logger = logging.getLogger(__name__)


# MySQL client instance
db_client = mysql_client

logger.info("Using MySQL as structured database")

