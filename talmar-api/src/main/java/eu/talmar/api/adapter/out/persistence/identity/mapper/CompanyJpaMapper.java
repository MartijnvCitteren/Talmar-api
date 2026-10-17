package eu.talmar.api.adapter.out.persistence.identity.mapper;

import eu.talmar.api.adapter.out.persistence.identity.CompanyJpaEntity;
import eu.talmar.api.domain.identity.Company;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/** Maps between the {@code Company} aggregate and {@code CompanyJpaEntity}. */
@Mapper(
    componentModel = "spring",
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN)
public interface CompanyJpaMapper {

  CompanyJpaEntity toJpaEntity(Company company);

  Company toDomain(CompanyJpaEntity entity);
}
