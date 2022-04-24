package xyz.staffjoy.faraday.core.mappings;

import com.github.structlog4j.ILogger;
import com.github.structlog4j.SLoggerFactory;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import xyz.staffjoy.faraday.config.FaradayProperties;
import xyz.staffjoy.faraday.config.MappingProperties;
import xyz.staffjoy.faraday.core.http.HttpClientProvider;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.util.CollectionUtils.isEmpty;

/**
 * @Title: 路由映射抽象类
 * @Description: 提供根据请求信息获取映射信息
 * @Author: Vanguard
 * @Version: 1.0
 * @Date: 22/4/17
 */
public abstract class MappingsProvider {

    private static final ILogger log = SLoggerFactory.getLogger(MappingsProvider.class);

    protected final ServerProperties serverProperties;
    protected final FaradayProperties faradayProperties;
    protected final MappingsValidator mappingsValidator;
    protected final HttpClientProvider httpClientProvider;
    protected List<MappingProperties> mappings;

    public MappingsProvider(
            ServerProperties serverProperties,
            FaradayProperties faradayProperties,
            MappingsValidator mappingsValidator,
            HttpClientProvider httpClientProvider
    ) {
        this.serverProperties = serverProperties;
        this.faradayProperties = faradayProperties;
        this.mappingsValidator = mappingsValidator;
        this.httpClientProvider = httpClientProvider;
    }

    /**
     * 从请求信息获取映射的路由信息
     * @param originHost HOST
     * @param request
     * @return xyz.staffjoy.faraday.config.MappingProperties
     * @author Vanguard
     * @date 22/4/17 20:27
     */
    public MappingProperties resolveMapping(String originHost, HttpServletRequest request) {
        if (shouldUpdateMappings(request)) {
            updateMappings();
        }
        List<MappingProperties> resolvedMappings = mappings.stream()
                .filter(mapping -> originHost.toLowerCase().equals(mapping.getHost().toLowerCase()))
                .collect(Collectors.toList());
        if (isEmpty(resolvedMappings)) {
            return null;
        }
        return resolvedMappings.get(0);
    }

    /**
     * 根据路由映射信息
     * @param
     * @return void
     * @author Vanguard
     * @date 22/4/17 20:28
     */
    @PostConstruct
    protected synchronized void updateMappings() {
        List<MappingProperties> newMappings = retrieveMappings();
        mappingsValidator.validate(newMappings);
        mappings = newMappings;
        httpClientProvider.updateHttpClients(mappings);
        log.info("Destination mappings updated", mappings);
    }

    /**
     * 时候需要更新路由映射
     * @param request
     * @return boolean
     * @author Vanguard
     * @date 22/4/17 20:28
     */
    protected abstract boolean shouldUpdateMappings(HttpServletRequest request);

    /**
     * 根据路由映射定义获取路由映射信息
     * @param
     * @return java.util.List<xyz.staffjoy.faraday.config.MappingProperties>
     * @author Vanguard
     * @date 22/4/17 20:29
     */
    protected abstract List<MappingProperties> retrieveMappings();
}
