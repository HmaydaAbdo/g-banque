package staport.finance.gBanque.logs.mappers;


import org.springframework.stereotype.Component;
import staport.finance.gBanque.logs.dtos.LogEntryDetailResponse;
import staport.finance.gBanque.logs.dtos.LogResponse;
import staport.finance.gBanque.logs.entities.Log;
import staport.finance.gBanque.logs.entities.LogEntryDetail;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class LogMapper {

    public LogResponse toDto(Log log) {
        if (log == null) {
            return null;
        }

        List<LogEntryDetailResponse> detailResponses = log.getLogDetails().stream()
                .map(this::toDetailDto)
                .collect(Collectors.toList());

        LogResponse dto = new LogResponse();
        dto.setId(log.getId());
        dto.setTimestamp(log.getTimestamp());
        dto.setUserName(log.getUserName());
        dto.setOperationType(log.getOperationType());
        dto.setEntityName(log.getEntityName());
        dto.setEntityId(log.getEntityId());
        dto.setLogDetails(detailResponses);
        return dto;
    }

    public List<LogResponse> toDtoList(List<Log> logs) {
        return logs.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public LogEntryDetailResponse toDetailDto(LogEntryDetail detail) {
        if (detail == null) {
            return null;
        }
        LogEntryDetailResponse dto = new LogEntryDetailResponse();
        dto.setId(detail.getId());
        dto.setFieldName(detail.getFieldName());
        dto.setOldValue(detail.getOldValue());
        dto.setNewValue(detail.getNewValue());
        return dto;
    }
}