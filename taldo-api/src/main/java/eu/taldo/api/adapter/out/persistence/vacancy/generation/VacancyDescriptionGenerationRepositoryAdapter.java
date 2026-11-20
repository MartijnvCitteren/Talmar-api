package eu.taldo.api.adapter.out.persistence.vacancy.generation;

import eu.taldo.api.adapter.out.persistence.vacancy.generation.mapper.VacancyDescriptionGenerationJpaMapper;
import eu.taldo.api.application.port.out.vacancy.generation.VacancyDescriptionGenerationRepository;
import eu.taldo.api.domain.vacancy.generation.VacancyDescriptionGeneration;
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
