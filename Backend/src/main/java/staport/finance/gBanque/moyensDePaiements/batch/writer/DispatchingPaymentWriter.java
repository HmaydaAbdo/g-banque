package staport.finance.gBanque.moyensDePaiements.batch.writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A generic, dispatching ItemWriter that delegates the writing of
 * different item types to specialized, single-purpose writers.
 * This approach eliminates the need for 'instanceof' checks and
 * simplifies the addition of new item types.
 */
public class DispatchingPaymentWriter implements ItemWriter<Object> {

    private static final Logger log = LoggerFactory.getLogger(DispatchingPaymentWriter.class);
    private final Map<Class<?>, ItemWriter<?>> writers;

    public DispatchingPaymentWriter(Map<Class<?>, ItemWriter<?>> writers) {
        this.writers = writers;
    }

    @Override
    public void write(Chunk<? extends Object> chunk) throws Exception {
        log.info("Dispatching chunk of {} items for writing.", chunk.size());

        // Aplatir toutes les listes et filtrer les nulls
        List<Object> items = chunk.getItems().stream()
                .flatMap(item -> {
                    if (item == null) return Stream.empty();
                    if (item instanceof List<?> list) return list.stream().filter(Objects::nonNull);
                    return Stream.of(item);
                })
                .toList();

        log.info("Flattened chunk contains {} items after flattening.", items.size());

        // Grouper par type et déléguer aux writers spécifiques
        Map<Class<?>, List<Object>> itemsByClass = items.stream()
                .collect(Collectors.groupingBy(Object::getClass));

        for (Map.Entry<Class<?>, List<Object>> entry : itemsByClass.entrySet()) {
            Class<?> itemClass = entry.getKey();
            List<Object> subList = entry.getValue();

            @SuppressWarnings("unchecked")
            ItemWriter<Object> writer = (ItemWriter<Object>) writers.get(itemClass);
            if (writer != null) {
                log.info("Saving {} entities of type {}.", subList.size(), itemClass.getSimpleName());
                writer.write(new Chunk<>(subList));
            } else {
                log.warn("No writer found for type {}. Skipping {} items.", itemClass.getSimpleName(), subList.size());
            }
        }
    }

}
