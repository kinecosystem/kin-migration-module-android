package kin.sdk.migration.common.interfaces;

import java.math.BigDecimal;

public interface IBalance {

    /**
     * @return BigDecimal the balance value
     */
    BigDecimal value();

    /**
     * @param precision the number of decimals points
     * @return String the balance value as a string with specified precision
     */
    String value(int precision);

}
