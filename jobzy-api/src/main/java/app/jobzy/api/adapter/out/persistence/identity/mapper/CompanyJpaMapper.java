package app.jobzy.api.adapter.out.persistence.identity.mapper;

import app.jobzy.api.adapter.out.persistence.identity.CompanyJpaEntity;
import app.jobzy.api.domain.identity.Company;
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
