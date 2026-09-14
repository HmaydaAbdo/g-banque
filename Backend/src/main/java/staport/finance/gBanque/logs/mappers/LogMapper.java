package staport.finance.gBanque.logs.mappers;

import org.mapstruct.Mapper;
import staport.finance.gBanque.logs.dtos.LogEntryDetailResponse;
import staport.finance.gBanque.logs.dtos.LogResponse;
import staport.finance.gBanque.logs.entities.Log;
import staport.finance.gBanque.logs.entities.LogEntryDetail;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LogMapper {

    LogResponse toDto(Log log);

    List<LogResponse> toDtoList(List<Log> logs);

    LogEntryDetailResponse toDetailDto(LogEntryDetail detail);
}
