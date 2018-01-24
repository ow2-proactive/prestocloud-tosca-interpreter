package prestocloud.tosca.context;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import prestocloud.component.ICSARRepositorySearchService;

/**
 * Manage tosca context
 */
@Service
public class ToscaContextInjector {
    @Resource
    public void setCsarRepositorySearchService(ICSARRepositorySearchService csarRepositorySearchService) {
        ToscaContext.setCsarRepositorySearchService(csarRepositorySearchService);
    }
}
