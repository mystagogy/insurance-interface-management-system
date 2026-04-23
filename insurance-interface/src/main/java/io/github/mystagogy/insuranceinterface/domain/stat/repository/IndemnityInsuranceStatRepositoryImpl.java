package io.github.mystagogy.insuranceinterface.domain.stat.repository;

import io.github.mystagogy.insuranceinterface.domain.stat.entity.IndemnityInsuranceStat;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.stereotype.Repository;

@Repository
public class IndemnityInsuranceStatRepositoryImpl implements IndemnityInsuranceStatRepositoryCustom {

    private static final String UPSERT_SQL = """
        insert into indemnity_insurance_stat (
            api_id,
            stat_date,
            age_group,
            gender,
            indemnity_type,
            coverage_item,
            premium_amount,
            raw_data,
            created_at,
            updated_at
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        on duplicate key update
            premium_amount = values(premium_amount),
            raw_data = values(raw_data),
            updated_at = values(updated_at)
        """;

    private final JdbcTemplate jdbcTemplate;

    public IndemnityInsuranceStatRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void upsertAll(List<IndemnityInsuranceStat> stats) {
        if (stats.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(UPSERT_SQL, stats, 300, new IndemnityInsuranceStatPreparedStatementSetter());
    }

    private static class IndemnityInsuranceStatPreparedStatementSetter
        implements ParameterizedPreparedStatementSetter<IndemnityInsuranceStat> {

        @Override
        public void setValues(PreparedStatement ps, IndemnityInsuranceStat stat) throws SQLException {
            LocalDateTime now = LocalDateTime.now();

            ps.setLong(1, stat.getApiInfo().getId());
            ps.setObject(2, stat.getStatDate());
            ps.setString(3, stat.getAgeGroup());
            ps.setString(4, stat.getGender().name());
            ps.setString(5, stat.getIndemnityType());
            ps.setString(6, stat.getCoverageItem());

            if (stat.getPremiumAmount() == null) {
                ps.setNull(7, Types.DECIMAL);
            } else {
                ps.setBigDecimal(7, stat.getPremiumAmount());
            }

            ps.setString(8, stat.getRawData());
            ps.setObject(9, now);
            ps.setObject(10, now);
        }
    }
}
