package io.github.mystagogy.insuranceinterface.domain.stat.repository;

import io.github.mystagogy.insuranceinterface.domain.stat.entity.LifeInsuranceStat;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.stereotype.Repository;

@Repository
public class LifeInsuranceStatRepositoryImpl implements LifeInsuranceStatRepositoryCustom {

    private static final String UPSERT_SQL = """
        insert into life_insurance_stat (
            api_id,
            stat_date,
            area_name,
            age_group,
            gender,
            insurance_type,
            subscription_count,
            subscription_rate,
            raw_data,
            created_at,
            updated_at
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        on duplicate key update
            subscription_count = values(subscription_count),
            subscription_rate = values(subscription_rate),
            raw_data = values(raw_data),
            updated_at = values(updated_at)
        """;

    private final JdbcTemplate jdbcTemplate;

    public LifeInsuranceStatRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void upsertAll(List<LifeInsuranceStat> stats) {
        if (stats.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(UPSERT_SQL, stats, 300, new LifeInsuranceStatPreparedStatementSetter());
    }

    private static class LifeInsuranceStatPreparedStatementSetter
        implements ParameterizedPreparedStatementSetter<LifeInsuranceStat> {

        @Override
        public void setValues(PreparedStatement ps, LifeInsuranceStat stat) throws SQLException {
            LocalDateTime now = LocalDateTime.now();

            ps.setLong(1, stat.getApiInfo().getId());
            ps.setObject(2, stat.getStatDate());
            ps.setString(3, stat.getAreaName());
            ps.setString(4, stat.getAgeGroup());
            ps.setString(5, stat.getGender().name());
            ps.setString(6, stat.getInsuranceType());
            ps.setLong(7, stat.getSubscriptionCount());

            if (stat.getSubscriptionRate() == null) {
                ps.setNull(8, Types.DECIMAL);
            } else {
                ps.setBigDecimal(8, stat.getSubscriptionRate());
            }

            ps.setString(9, stat.getRawData());
            ps.setObject(10, now);
            ps.setObject(11, now);
        }
    }
}
