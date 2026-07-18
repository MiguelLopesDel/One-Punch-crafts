package com.onepunchcrafts.runtime.ability;

import com.onepunchcrafts.api.Id;
import com.onepunchcrafts.api.ability.Timeline;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbilityBookTest {
    @Test
    void emitsFromOneServerTickClockWithoutReconsultingSelection() {
        Id normal = Id.parse("onepunchcrafts:strike/normal_punch");
        Timeline timeline = Timeline.builder(Id.parse("onepunchcrafts:timeline/consecutive_normal"), 10)
                .at(2, new Timeline.Command.StrikeArea(normal, 5, 3))
                .at(4, new Timeline.Command.StrikeArea(normal, 5, 3))
                .build();
        AbilityBook book = new AbilityBook();
        book.start(timeline, 100, null, List.of(), java.util.Map.of());
        book.select(Id.parse("onepunchcrafts:ability/serious_punch"));

        List<AbilityBook.Emission> emissions = new ArrayList<>();
        book.tick(101, emissions::add);
        book.tick(104, emissions::add);

        assertEquals(2, emissions.size());
        assertEquals(normal, ((Timeline.Command.StrikeArea) emissions.get(0).step().command()).strikeId());
    }
}
