/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package x7;

import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import x7.core.bean.*;
import x7.repository.BaseRepository;
import x7.repository.DataRepository;
import x7.repository.Repository;
import x7.repository.RepositoryBooter;
import x7.repository.schema.SchemaConfig;
import x7.repository.schema.SchemaTransformRepository;
import x7.repository.schema.customizer.SchemaTransformCustomizer;
import x7.repository.schema.customizer.SchemaTransformRepositoryBuilder;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class RepositoryListener implements
        ApplicationListener<ApplicationStartedEvent> {


    @Override
    public void onApplicationEvent(ApplicationStartedEvent applicationStartedEvent) {

        List<Class<? extends BaseRepository>> clzzList = null;
        if (SchemaConfig.isSchemaTransformEnabled) {
            clzzList = customizeSchemaTransform(applicationStartedEvent);
        }

        RepositoryBooter.onStarted();

        if (clzzList != null){

            for (Class<? extends BaseRepository> clzz : clzzList) {

                DataRepository dataRepository = (DataRepository) applicationStartedEvent.getApplicationContext().getBean(Repository.class);

                List list = list(dataRepository, clzz);//查出所有配置
                if (!list.isEmpty()) {
                    reparse(list);
                }
            }
        }
    }

    private List<Class<? extends BaseRepository>> customizeSchemaTransform(ApplicationStartedEvent applicationStartedEvent){


        SchemaTransformCustomizer customizer = null;
        try {
            customizer = applicationStartedEvent.getApplicationContext().getBean(SchemaTransformCustomizer.class);
        }catch (Exception e){
        }

        if (customizer != null) {
            SchemaTransformRepositoryBuilder builder = new SchemaTransformRepositoryBuilder();
           return customizer.customize(builder);
        }

        SchemaTransformRepositoryBuilder.registry = null;

        List<Class<? extends BaseRepository>> list = new ArrayList<>();
        list.add(SchemaTransformRepository.class);
        return list;
    }


    private void reparse(List list) {

        //key: originTable
        Map<String,List<TransformConfigurable>> map = new HashMap<>();

        for (Object obj : list) {
            if (obj instanceof TransformConfigurable) {

                TransformConfigurable transformed = (TransformConfigurable) obj;
                String originTable = transformed.getOriginTable();
                List<TransformConfigurable> transformedList = map.get(originTable);
                if (transformedList == null){
                    transformedList = new ArrayList<>();
                    map.put(originTable,transformedList);
                }
                transformedList.add(transformed);
            }
        }

        for (Map.Entry<String,List<TransformConfigurable>> entry : map.entrySet()){
            String originTable = entry.getKey();

            Parsed parsed = Parser.getByTableName(originTable);
            if (parsed == null)
                continue;

            List<TransformConfigurable> transformedList = entry.getValue();
            for (TransformConfigurable transformed : transformedList) {
                parsed.setTableName(transformed.getTargetTable());//FIXME 直接替换了原始的表
                parsed.setTransforemedAlia(transformed.getAlia());

                for (BeanElement be : parsed.getBeanElementList()){
                    if (be.getMapper().equals(transformed.getOriginColumn())){
                        be.mapper = transformed.getTargetColumn();//FIXME 直接替换了原始的列, 只需要目标对象的属性有值
                        break;
                    }
                }
            }

            parsed.reset(parsed.getBeanElementList());
            String tableName = parsed.getTableName();
            Parsed parsedTransformed = Parser.getByTableName(tableName);
            parsed.setParsedTransformed(parsedTransformed);

            SchemaConfig.transformableSet.add(parsed.getClz());
        }
    }

    private List list(DataRepository dataRepository,Class<? extends BaseRepository> clzz) {

        Type[] types = clzz.getGenericInterfaces();

        ParameterizedType parameterized = (ParameterizedType) types[0];
        Class clazz = (Class) parameterized.getActualTypeArguments()[0];

        List list = dataRepository.list(clazz);

        return list;
    }

}
