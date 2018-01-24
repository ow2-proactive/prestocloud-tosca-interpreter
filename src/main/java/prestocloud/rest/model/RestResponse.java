package prestocloud.rest.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RestResponse<T> {
    private T data;
    private RestError error;
}