package staport.finance.gBanque.cmt.dtos;

public class DeleteResponceDto {
    String msg;
    public DeleteResponceDto() {
    }
    public DeleteResponceDto(String msg) {
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
