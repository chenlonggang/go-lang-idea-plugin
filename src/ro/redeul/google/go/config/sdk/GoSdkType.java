package ro.redeul.google.go.config.sdk;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.projectRoots.*;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import ro.redeul.google.go.GoBundle;
import ro.redeul.google.go.GoIcons;
import ro.redeul.google.go.sdk.GoSdkUtil;
import ro.redeul.google.go.util.GoUtil;

import javax.swing.*;

public class GoSdkType extends SdkType {

    public GoSdkType() {
        super("Google Go SDK");
    }

    public static GoSdkType getInstance() {
        return SdkType.findInstance(GoSdkType.class);
    }

    @Override
    public String suggestHomePath() {
        return GoUtil.resolvePotentialGoogleGoHomePath();
    }

    @Override
    public FileChooserDescriptor getHomeChooserDescriptor() {
        final FileChooserDescriptor descriptor = new FileChooserDescriptor(true, true, false, false, false, false) {
          public void validateSelectedFiles(VirtualFile[] files) throws Exception {
            if (files.length != 0){
              final String selectedPath = files[0].getPath();
              boolean valid = isValidSdkHome(selectedPath);
              if (!valid){
                valid = isValidSdkHome(adjustSelectedSdkHome(selectedPath));
                if (!valid) {
                  String message = files[0].isDirectory()
                                   ? ProjectBundle.message("sdk.configure.home.invalid.error", getPresentableName())
                                   : ProjectBundle.message("sdk.configure.home.file.invalid.error", getPresentableName());
                  throw new Exception(message);
                }
              }
            }
          }
        };

        descriptor.setTitle(GoBundle.message("go.sdk.configure.title", getPresentableName()));
        return descriptor;
    }

    @Override
    public boolean isValidSdkHome(String path) {
        String[] stringList = GoSdkUtil.testGoogleGoSdk(path);

        return GoSdkUtil.validateSdkTestingResult(stringList, path);
    }

    @Override
    public Icon getIcon() {
        return GoIcons.GO_ICON_16x16;
    }

    @Override
    public Icon getIconForAddAction() {
        return GoIcons.GO_ICON_16x16;
    }

    @Override
    public Icon getIconForExpandedTreeNode() {
        return GoIcons.GO_ICON_16x16;
    }

    @Override
    public String suggestSdkName(String currentSdkName, String sdkHome) {

        String name = "Go Sdk";
        if ( sdkHome.matches(".*bundled/go-sdk/?$") ) {
            name = "Bundled Go Sdk";
        }

        return name;
//        return "Go" + (sdkData != null && sdkData.VERSION != null && sdkData.VERSION.trim().length() > 0 ? " (" + sdkData.VERSION + ")" : "");
//        return "Go Sdk";
    }

    @Override
    public String getVersionString(String sdkHome) {
        return super.getVersionString(sdkHome);
    }

    @Override
    public AdditionalDataConfigurable createAdditionalDataConfigurable(SdkModel sdkModel, SdkModificator sdkModificator) {
        return null;
    }

    @Override
    public void setupSdkPaths(Sdk sdk) {
        VirtualFile homeDirectory = sdk.getHomeDirectory();

        if (sdk.getSdkType() != this || homeDirectory == null) {
            return;
        }

        String path = homeDirectory.getPath();

        String[] stringList = GoSdkUtil.testGoogleGoSdk(path);

        if ( ! GoSdkUtil.validateSdkTestingResult(stringList, path) ) {
            return;
        }

        GoSdkData sdkData = new GoSdkData();

        sdkData.BINARY_PATH = stringList[1];
        sdkData.TARGET_OS = stringList[2];
        sdkData.TARGET_ARCH = stringList[3];
        sdkData.VERSION = stringList[4];

        final VirtualFile librariesRoot = homeDirectory.findFileByRelativePath(String.format("pkg/%s_%s/", sdkData.TARGET_OS, sdkData.TARGET_ARCH));
        final VirtualFile sourcesRoot = homeDirectory.findFileByRelativePath("src/pkg/");

        if (librariesRoot != null) {
            librariesRoot.refresh(false, false);
        }
        if (sourcesRoot != null) {
            sourcesRoot.refresh(false, false);
        }

        final SdkModificator sdkModificator = sdk.getSdkModificator();
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            public void run() {
                sdkModificator.addRoot(librariesRoot, OrderRootType.CLASSES);
                sdkModificator.addRoot(sourcesRoot, OrderRootType.CLASSES);
                sdkModificator.addRoot(sourcesRoot, OrderRootType.SOURCES);
            }
        });

        sdkModificator.setVersionString(sdkData.VERSION);
        sdkModificator.setSdkAdditionalData(sdkData);
        sdkModificator.commitChanges();
    }

    @Override
    public SdkAdditionalData loadAdditionalData(Sdk currentSdk, Element additional) {
        return XmlSerializer.deserialize(additional, GoSdkData.class);
    }

    @Override
    public void saveAdditionalData(SdkAdditionalData additionalData, Element additional) {
        if (additionalData instanceof GoSdkData) {
            XmlSerializer.serializeInto(additionalData, additional);
        }
    }

    @Override
    public String getPresentableName() {
        return "Go Sdk";
    }

    @Override
    public String getVersionString(Sdk sdk) {
        return sdk.getVersionString();
    }

    @Override
    public boolean isRootTypeApplicable(OrderRootType type) {
        return type == OrderRootType.CLASSES || type == OrderRootType.SOURCES;
    }

    public static boolean isInstance(Sdk sdk) {
        return sdk != null && sdk.getSdkType() == GoSdkType.getInstance();
    }
}
