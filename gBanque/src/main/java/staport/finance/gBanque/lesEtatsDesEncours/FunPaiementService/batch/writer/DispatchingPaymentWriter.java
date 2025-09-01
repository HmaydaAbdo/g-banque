package staport.finance.gBanque.lesEtatsDesEncours.FunPaiementService.batch.writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        // Group items by their class type
        Map<Class<?>, List<Object>> itemsByClass = chunk.getItems().stream()
                .collect(Collectors.groupingBy(Object::getClass));

        // Iterate through the grouped lists and delegate to the appropriate writer
        for (Map.Entry<Class<?>, List<Object>> entry : itemsByClass.entrySet()) {
            Class<?> itemClass = entry.getKey();
            List<Object> subList = entry.getValue();

            ItemWriter<Object> writer = (ItemWriter<Object>) writers.get(itemClass);
            if (writer != null) {
                // Delegate writing to the correct specialized writer, creating a new chunk
                log.info("Saving {} entities of type {}.", subList.size(), itemClass.getSimpleName());
                writer.write(new Chunk<>(subList));
            } else {
                log.warn("No specific writer found for class {}. Skipping {} items.", itemClass.getSimpleName(), subList.size());
            }
        }
    }
}
