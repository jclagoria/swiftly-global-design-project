package com.swiftly.service.user.data;

import com.swiftly.service.user.api.dto.UpdateUserRequest;
import lombok.Builder;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.api.Randomizer;
import org.jeasy.random.randomizers.PhoneNumberRandomizer;
import org.jeasy.random.randomizers.StreetRandomizer;

import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

import static org.jeasy.random.FieldPredicates.*;

@Builder
public class UpdateUserRequestBuilder {

    private static final List<String> ARGENTINA_ZONES = ZoneId.getAvailableZoneIds()
            .stream().filter(zone -> zone.startsWith("America/Argentina")).toList();
    private static final List<String> LOCALES = List.of(
            "en-US", "en-GB", "es-ES", "es-MX", "fr-FR", "de-DE", "it-IT", "pt-BR", "zh-CN", "ja-JP",
            "ar-SA", "ru-RU", "hi-IN", "ko-KR", "tr-TR", "nl-NL", "sv-SE", "da-DK", "fi-FI"
    );

    private static final EasyRandom easyRandom;
    static {
        long seed = ThreadLocalRandom.current().nextLong();
        Locale arLocale = new Locale("es", "AR");

        Randomizer<String> phoneRandomizer = new PhoneNumberRandomizer(seed, arLocale);
        Randomizer<String> addressRandomizer = new StreetRandomizer(seed, arLocale);
        Randomizer<String> localeRandomizer = () -> LOCALES
                .get(ThreadLocalRandom.current().nextInt(LOCALES.size()));
        Randomizer<String> timezoneRandomizer = () -> ARGENTINA_ZONES.get(
                ThreadLocalRandom.current().nextInt(ARGENTINA_ZONES.size())
        );

        EasyRandomParameters params = new EasyRandomParameters()
                .randomize(named("phone").and(ofType(String.class)).and(inClass(UpdateUserRequest.class)),
                        phoneRandomizer)
                .randomize(named("address").and(ofType(String.class)).and(inClass(UpdateUserRequest.class)),
                        addressRandomizer)
                .randomize(named("locale").and(ofType(String.class)).and(inClass(UpdateUserRequest.class)),
                        localeRandomizer)
                .randomize(named("timezone").and(ofType(String.class)).and(inClass(UpdateUserRequest.class)),
                        timezoneRandomizer);
        easyRandom = new EasyRandom(params);
    }

    @Builder.Default
    private String firstName = easyRandom.nextObject(String.class);

    @Builder.Default
    private String lastName = easyRandom.nextObject(String.class);

    @Builder.Default
    private String phone = easyRandom.nextObject(String.class);

    @Builder.Default
    private String address = easyRandom.nextObject(String.class);

    @Builder.Default
    private String locale = easyRandom.nextObject(String.class);

    @Builder.Default
    private String timezone = easyRandom.nextObject(String.class);

    /**
     * Build an instance of {@link UpdateUserRequest} with the fields set according to
     * the builder configuration.
     *
     * @return an instance of {@link UpdateUserRequest}
     */
    public UpdateUserRequest build() {
        return new UpdateUserRequest(
                firstName,
                lastName,
                phone,
                address,
                locale,
                timezone
        );
    }

    /**
     * Return an instance of the builder with all fields randomly set.
     * @return an instance of the builder with all fields randomly set.
     */
    public static UpdateUserRequestBuilder random() {
        return easyRandom.nextObject(UpdateUserRequestBuilder.class);
    }


}
