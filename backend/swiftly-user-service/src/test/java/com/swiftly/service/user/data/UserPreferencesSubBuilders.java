package com.swiftly.service.user.data;


import com.swiftly.service.user.adapter.out.persistence.entities.mongo.UserPreferencesEntity;
import lombok.Builder;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.api.Randomizer;

import java.util.concurrent.ThreadLocalRandom;

import static org.jeasy.random.FieldPredicates.*;

public class UserPreferencesSubBuilders {

    @Builder
    public static class NotificationsBuilder {
        private final static EasyRandom easyRandom;

        static {
            EasyRandomParameters parameters = new EasyRandomParameters();
            easyRandom = new EasyRandom(parameters);
        }

        private final boolean email = easyRandom.nextBoolean();
        private final boolean sms = easyRandom.nextBoolean();
        private final boolean push = easyRandom.nextBoolean();
        private final boolean inApp = easyRandom.nextBoolean();

        public UserPreferencesEntity.Notifications build() {
            return new UserPreferencesEntity.Notifications(email, sms, push, inApp);
        }

        public static NotificationsBuilder random() {
            return easyRandom.nextObject(NotificationsBuilder.class);
        }
    }

    @Builder
    public static class PreferencesBuilder {
        private final static EasyRandom easyRandom;
        static {
            // Customize maxTransactionAmt range
            Randomizer<Double> amtRandomizer = () -> ThreadLocalRandom
                    .current().nextDouble(0.0, 10000.0);
            EasyRandomParameters params = new EasyRandomParameters()
                    .randomize(named("maxTransactionAmt").and(ofType(Double.class))
                                    .and(inClass(PreferencesBuilder.class)),
                            amtRandomizer);
            easyRandom = new EasyRandom(params);
        }

        @Builder.Default
        private final boolean dailyReport   = easyRandom.nextObject(Boolean.class);
        @Builder.Default
        private final boolean fraudAlerts   = easyRandom.nextObject(Boolean.class);
        @Builder.Default
        private final Double  maxTransactionAmt = easyRandom.nextObject(Double.class);

        /**
         * Builds a UserPreferencesEntity.Preferences instance.
         */
        public UserPreferencesEntity.Preferences build() {
            return new UserPreferencesEntity.Preferences(dailyReport, fraudAlerts, maxTransactionAmt);
        }

        /**
         * Returns a builder pre-populated with random values.
         */
        public static PreferencesBuilder random() {
            return easyRandom.nextObject(PreferencesBuilder.class);
        }
    }

}
