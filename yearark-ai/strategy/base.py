from abc import ABC, abstractmethod
from models.request import GenerationRequest
from models.result import GenerationResult


class GroupingStrategy(ABC):

    @abstractmethod
    def process(self, request: GenerationRequest) -> GenerationResult:
        pass
