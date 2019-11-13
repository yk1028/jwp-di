package nextstep.di.factory;

import com.google.common.collect.Maps;
import nextstep.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.stream.Collectors;

public class BeanFactory {
    private static final Logger logger = LoggerFactory.getLogger(BeanFactory.class);

    private Map<Class<?>, Object> beans = Maps.newHashMap();

    protected BeanFactory() {
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> requiredType) {
        return (T) beans.get(requiredType);
    }

    public Set<Class<?>> getAnnotatedClasses(Class<? extends Annotation> annotation) {
        return beans.keySet().stream()
                .filter(clazz -> clazz.isAnnotationPresent(annotation))
                .collect(Collectors.toSet());
    }

    protected void addBeans(Set<Class<?>> preInstantiateBeans) {
        preInstantiateBeans.forEach(ExceptionUtils.consumerWrapper(bean -> {
            logger.debug(bean.getName());

            if (!beans.containsKey(bean)) {
                instantiateBean(bean, preInstantiateBeans);
            }
        }));
    }

    private void instantiateBean(Class<?> bean, Set<Class<?>> preInstantiateBeans) throws Exception {
        Constructor constructor = BeanFactoryUtils.findConstructor(bean);
        List<Class<?>> parameterTypes = getConcreteParameterTypes(constructor, preInstantiateBeans);
        List<Object> parameterInstances = getParameterInstances(parameterTypes, preInstantiateBeans);

        logger.debug("Create : {}", bean.getName());
        beans.put(bean, constructor.newInstance(parameterInstances.toArray()));
    }

    private List<Class<?>> getConcreteParameterTypes(Constructor constructor, Set<Class<?>> preInstantiateBeans) {
        return Arrays.stream(constructor.getParameterTypes())
                .map(paramType -> BeanFactoryUtils.findConcreteClass(paramType, preInstantiateBeans))
                .collect(Collectors.toList());
    }

    private List<Object> getParameterInstances(List<Class<?>> parameterTypes, Set<Class<?>> preInstantiateBeans) throws Exception {
        List<Object> params = new ArrayList<>();

        for (Class parameterType : parameterTypes) {
            logger.debug("ParameterType : {}", parameterType.getName());

            if (!beans.containsKey(parameterType)) {
                instantiateBean(parameterType, preInstantiateBeans);
            }
            params.add(beans.get(parameterType));
        }
        return params;
    }
}
