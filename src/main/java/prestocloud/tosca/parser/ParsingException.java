package prestocloud.tosca.parser;

import java.util.List;

import com.google.common.collect.Lists;

import lombok.Getter;
import lombok.Setter;
import prestocloud.exceptions.FunctionalException;

@Getter
@Setter
public class ParsingException extends FunctionalException {
    private static final long serialVersionUID = 1L;

    private String fileName;
    private final List<ParsingError> parsingErrors;

    public ParsingException(String fileName, ParsingError toscaParsingError) {
        super(toscaParsingError.toString());
        this.fileName = fileName;
        parsingErrors = Lists.newArrayList(toscaParsingError);
    }

    public ParsingException(String fileName, List<ParsingError> toscaParsingErrors) {
        super(toscaParsingErrors.toString());
        this.fileName = fileName;
        parsingErrors = toscaParsingErrors;
    }
}