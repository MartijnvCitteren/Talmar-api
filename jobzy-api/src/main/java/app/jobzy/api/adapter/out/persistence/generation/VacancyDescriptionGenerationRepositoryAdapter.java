package app.jobzy.api.adapter.out.persistence.generation;

import app.jobzy.api.adapter.out.persistence.generation.mapper.VacancyDescriptionGenerationJpaMapper;
import app.jobzy.api.application.port.out.VacancyDescriptionGenerationRepository;
import app.jobzy.api.domain.generation.VacancyDescriptionGeneration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Persistence adapter for {@link VacancyDescriptionGenerationRepository}. */
@Component
@RequiredArgsConstructor
public class VacancyDescriptionGenerationRepositoryAdapter
    implements VacancyDescriptionGenerationRepository {
  private final VacancyDescriptionGenerationJpaMapper jpaMapper;
  private final VacancyDescriptionGenerationJpaRepository jpaRepository;

  @Override
  public void save(VacancyDescriptionGeneration generation) {
    jpaRepository.save(jpaMapper.toJpaEntity(generation));
  }

  @Override
  public Optional<VacancyDescriptionGeneration> findById(UUID id) {
    return jpaRepository.findById(id).map(jpaMapper::toDomain);
  }

  @Override
  public int deleteRequestedBefore(LocalDateTime cutoff) {
    return jpaRepository.deleteByRequestedAtBefore(cutoff);
  }
}
