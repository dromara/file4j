package org.dromara.x.file.storage.solon;

import static org.dromara.x.file.storage.core.FileStorageServiceBuilder.doesNotExistClass;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.FileStorageServiceBuilder;
import org.dromara.x.file.storage.core.aspect.FileStorageAspect;
import org.dromara.x.file.storage.core.file.FileWrapperAdapter;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.platform.FileStorageClientFactory;
import org.dromara.x.file.storage.core.recorder.DefaultFileRecorder;
import org.dromara.x.file.storage.core.recorder.FileRecorder;
import org.dromara.x.file.storage.core.tika.ContentTypeDetect;
import org.dromara.x.file.storage.core.tika.DefaultTikaFactory;
import org.dromara.x.file.storage.core.tika.TikaContentTypeDetect;
import org.dromara.x.file.storage.core.tika.TikaFactory;
import org.dromara.x.file.storage.solon.SolonFileStorageProperties.SolonLocalConfig;
import org.dromara.x.file.storage.solon.SolonFileStorageProperties.SolonLocalPlusConfig;
import org.dromara.x.file.storage.solon.file.UploadedFileWrapperAdapter;
import org.noear.solon.Solon;
import org.noear.solon.annotation.*;

/**
 * @author link2fun
 */
@Slf4j
@Configuration
@Condition(onMissingBean = FileStorageService.class)
public class FileStorageAutoConfiguration {

    @Bean(typed = true)
    public SolonFileStorageProperties solonFileStorageProperties(
            @Inject("${dromara.x-file-storage}") SolonFileStorageProperties properties) {
        return properties;
    }

    /**
     * 当没有找到 FileRecorder 时使用默认的 FileRecorder
     */
    @Bean
    @Condition(onMissingBean = FileRecorder.class)
    public FileRecorder fileRecorder() {
        log.warn("没有找到 FileRecorder 的实现类，文件上传之外的部分功能无法正常使用，必须实现该接口才能使用完整功能！");
        return new DefaultFileRecorder();
    }

    /**
     * Tika 工厂类型，用于识别上传的文件的 MINE
     */
    @Bean
    @Condition(onMissingBean = TikaFactory.class)
    public TikaFactory tikaFactory() {
        return new DefaultTikaFactory();
    }

    /**
     * 识别文件的 MIME 类型
     */
    @Bean
    @Condition(onMissingBean = ContentTypeDetect.class)
    public ContentTypeDetect contentTypeDetect(TikaFactory tikaFactory) {
        return new TikaContentTypeDetect(tikaFactory);
    }

    /**
     * 文件存储服务
     */
    //  @Bean(destroyMethod = "destroy")
    @Bean
    public FileStorageService fileStorageService(
            @Inject SolonFileStorageProperties properties,
            FileRecorder fileRecorder,
            @Inject(required = false) List<List<? extends FileStorage>> fileStorageLists,
            @Inject(required = false) List<FileStorageAspect> aspectList,
            @Inject(required = false) List<FileWrapperAdapter> fileWrapperAdapterList,
            ContentTypeDetect contentTypeDetect,
            @Inject(required = false) List<List<FileStorageClientFactory<?>>> clientFactoryList) {

        if (fileStorageLists == null) {
            fileStorageLists = new ArrayList<>();
        }
        if (aspectList == null) {
            aspectList = new ArrayList<>();
        }
        if (fileWrapperAdapterList == null) {
            fileWrapperAdapterList = new ArrayList<>();
        }
        if (clientFactoryList == null) {
            clientFactoryList = new ArrayList<>();
        }

        FileStorageServiceBuilder builder = FileStorageServiceBuilder.create(properties.toFileStorageProperties())
                .setFileRecorder(fileRecorder)
                .setAspectList(aspectList)
                .setContentTypeDetect(contentTypeDetect)
                .setFileWrapperAdapterList(fileWrapperAdapterList)
                .setClientFactoryList(clientFactoryList);

        fileStorageLists.forEach(builder::addFileStorage);

        if (properties.getEnableByteFileWrapper()) {
            builder.addByteFileWrapperAdapter();
        }
        if (properties.getEnableUriFileWrapper()) {
            builder.addUriFileWrapperAdapter();
        }
        if (properties.getEnableInputStreamFileWrapper()) {
            builder.addInputStreamFileWrapperAdapter();
        }
        if (properties.getEnableLocalFileWrapper()) {
            builder.addLocalFileWrapperAdapter();
        }
        if (properties.getEnableHttpServletRequestFileWrapper()) {
            if (doesNotExistClass("javax.servlet.http.HttpServletRequest")
                    && doesNotExistClass("jakarta.servlet.http.HttpServletRequest")) {
                log.warn(
                        "当前未检测到 Servlet 环境，无法加载 HttpServletRequest 的文件包装适配器，请将参数【dromara.x-file-storage.enable-http-servlet-request-file-wrapper】设置为 【false】来消除此警告");
            } else {
                builder.addHttpServletRequestFileWrapperAdapter();
            }
        }
        if (properties.getEnableMultipartFileWrapper()) {
            log.warn(
                    "当前为 Solon 环境，无需加载 MultipartFile 的文件包装适配器, 请将参数【dromara.x-file-storage.enable-multipart-file-wrapper】设置为 【false】来消除此警告");
        }

        if (properties.getEnableUploadedFileWrapper()) {
            if (doesNotExistClass("org.noear.solon.core.handle.UploadedFile")) {
                log.warn(
                        "当前未检测到 Solon 环境，无法加载 UploadedFile 的文件包装适配器，请将参数【dromara.x-file-storage.enable-uploaded-file-wrapper】设置为 【false】来消除此警告");
            } else {
                builder.addFileWrapperAdapter(new UploadedFileWrapperAdapter());
            }
        }

        if (doesNotExistClass("org.noear.solon.web.staticfiles.StaticMappings")) {
            long localAccessNum = properties.getLocal().stream()
                    .filter(SolonLocalConfig::getEnableStorage)
                    .filter(SolonLocalConfig::getEnableAccess)
                    .count();
            long localPlusAccessNum = properties.getLocalPlus().stream()
                    .filter(SolonLocalPlusConfig::getEnableStorage)
                    .filter(SolonLocalPlusConfig::getEnableAccess)
                    .count();

            if (localAccessNum + localPlusAccessNum > 0) {
                log.warn("当前未检测到 Solon staticfiles 模块，无法开启本地存储平台的本地访问功能，请将关闭本地访问来消除此警告");
            }
        }

        return builder.build();
    }

    /**
     * 对 FileStorageService 注入自己的代理对象，不然会导致针对 FileStorageService 的代理方法不生效
     */
    @Init
    public void onContextRefreshedEvent() {
        Solon.context().getBeanAsync(FileStorageService.class, (bean) -> {
            bean.setSelf(bean);
        });
    }
}
