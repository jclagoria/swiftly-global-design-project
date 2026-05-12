package com.swiftly.service.user.data;

import com.swiftly.service.user.domain.model.UserPreferencesModel;
import lombok.Builder;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.api.Randomizer;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.jeasy.random.FieldPredicates.*;

@Builder
public class UserPreferencesModelBuilder {

    private static final List<String> ARGENTINA_ZONES = ZoneId
            .getAvailableZoneIds().stream()
            .filter(zone -> zone.startsWith("America/Argentina")).toList();

    private static final List<String> LOCALES = List.of(
            "en-US", "en-GB", "es-ES", "es-MX", "fr-FR", "de-DE",
            "it-IT", "pt-BR", "zh-CN", "ja-JP", "ar-SA", "ru-RU",
            "hi-IN", "ko-KR", "tr-TR", "nl-NL", "sv-SE", "da-DK", "fi-FI"
    );

    private static final List<String> CURRENCIES = List.of(
            "USD", "EUR", "ARS", "BRL", "GBP", "JPY", "CNY", "INR"
    );

    private static final EasyRandom easyRandom;

    static {
        Randomizer<String> localeRandomizer = () -> LOCALES
                .get(ThreadLocalRandom.current().nextInt(LOCALES.size()));
        Randomizer<String> currencyRandomizer = () -> CURRENCIES
                .get(ThreadLocalRandom.current().nextInt(CURRENCIES.size()));
        Randomizer<String> timezoneRandomizer = () -> ARGENTINA_ZONES.get(
                ThreadLocalRandom.current().nextInt(ARGENTINA_ZONES.size())
        );

        EasyRandomParameters esyRandomParameters = new EasyRandomParameters()
                .randomize(named("locale").and(ofType(String.class))
                        .and(inClass(UserPreferencesModelBuilder.class)), localeRandomizer)
                .randomize(named("timezone").and(ofType(String.class))
                        .and(inClass(UserPreferencesModelBuilder.class)), timezoneRandomizer)
                .randomize(named("defaultCurrency").and(ofType(String.class))
                        .and(inClass(UserPreferencesModelBuilder.class)), currencyRandomizer);
        easyRandom = new EasyRandom(esyRandomParameters);
    }

    @Builder.Default
    private String id = UUID.randomUUID().toString();

    @Builder.Default
    private UUID userId = UUID.randomUUID();

    @Builder.Default
    private String language = easyRandom.nextObject(String.class);

    @Builder.Default
    private String timezone = easyRandom.nextObject(String.class);

    @Builder.Default
    private String defaultCurrency = easyRandom.nextObject(String.class);

    @Builder.Default
    private UserPreferencesModel.NotificationsModel notifications =
            easyRandom.nextObject(UserPreferencesModel.NotificationsModel.class);


    @Builder.Default
    private UserPreferencesModel.PreferencesModel preferences =
            easyRandom.nextObject(UserPreferencesModel.PreferencesModel.class);

    @Builder.Default
    private Instant createdAt =
            Instant.now().minusSeconds(ThreadLocalRandom.current().nextLong(0, 86400 * 30));

    @Builder.Default
    private Instant updatedAt = Instant.now();

    /**
     * Build a UserPreferencesEntity with the configured or randomized values.
     */
    public UserPreferencesModel build() {
        return new UserPreferencesModel(
                userId,
                language,
                timezone,
                defaultCurrency,
                notifications,
                preferences,
                createdAt,
                updatedAt
        );
    }
    
    /**
     * Return a builder instance with all fields randomly populated.
     */
    public static UserPreferencesModel random() {
        return easyRandom.nextObject(UserPreferencesModel.class);
    }


}
