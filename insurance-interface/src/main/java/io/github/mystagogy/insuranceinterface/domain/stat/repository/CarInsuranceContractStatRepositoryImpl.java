package io.github.mystagogy.insuranceinterface.domain.stat.repository;

import io.github.mystagogy.insuranceinterface.domain.stat.entity.CarInsuranceContractStat;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.stereotype.Repository;

@Repository
public class CarInsuranceContractStatRepositoryImpl implements CarInsuranceContractStatRepositoryCustom {

    private static final String UPSERT_SQL = """
        insert into car_insurance_contract_stat (
            api_id,
            base_ym,
            insurance_type,
            coverage_type,
            gender,
            age_group,
            car_origin_type,
            car_type,
            contract_count,
            earned_premium,
            raw_data,
            created_at,
            updated_at
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        on duplicate key update
            contract_count = values(contract_count),
            earned_premium = values(earned_premium),
            raw_data = values(raw_data),
            updated_at = values(updated_at)
        """;

    private final JdbcTemplate jdbcTemplate;

    public CarInsuranceContractStatRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void upsertAll(List<CarInsuranceContractStat> stats) {
        if (stats.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(UPSERT_SQL, stats, 300, new CarInsuranceContractStatPreparedStatementSetter());
    }

    private static class CarInsuranceContractStatPreparedStatementSetter
        implements ParameterizedPreparedStatementSetter<CarInsuranceContractStat> {

        @Override
        public void setValues(PreparedStatement ps, CarInsuranceContractStat stat) throws SQLException {
            LocalDateTime now = LocalDateTime.now();

            ps.setLong(1, stat.getApiInfo().getId());
            ps.setString(2, stat.getBaseYm());
            ps.setString(3, stat.getInsuranceType());
            ps.setString(4, stat.getCoverageType());
            ps.setString(5, stat.getGender().name());
            ps.setString(6, stat.getAgeGroup());
            ps.setString(7, stat.getCarOriginType());
            ps.setString(8, stat.getCarType());
            ps.setLong(9, stat.getContractCount());

            if (stat.getEarnedPremium() == null) {
                ps.setNull(10, Types.DECIMAL);
            } else {
                ps.setBigDecimal(10, stat.getEarnedPremium());
            }

            ps.setString(11, stat.getRawData());
            ps.setObject(12, now);
            ps.setObject(13, now);
        }
    }
}
