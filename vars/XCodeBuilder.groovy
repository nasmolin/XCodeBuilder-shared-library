#!/usr/bin/env groovy

/**     
 * Экранируем пробелы и скобки при выборе xcode workspace по умолчанию.
 * Вообще было бы неплохо одной регуляркой это делать, а не тремя, и по всем спец-символам, а не только пробел и скобки. 
 * sh command: find . -iname "*(IOS).xcworkspace"  | sed 's/\ /\\ /g' | sed 's/(/\\(/g' | sed 's/)/\\)/g'
 * after snnipet generator:
 * (returnStdout: true, script: 'find . -iname "*(IOS).xcworkspace"  | sed \'s/\\ /\\\\ /g\' | sed \'s/(/\\\\(/g\' | sed \'s/)/\\\\)/g\'').trim()
 * 
 * Для получения списка схем и выбора первой схемы в списке
 * sh command: xcodebuild -list -workspace ${xcode_workspace} -hideShellScriptEnvironment | sed -n '/Schemes:/,+1 p' | awk '! /Schemes:$/'
 * after snnipet generator:
 * (returnStdout: true, script: "xcodebuild -list -workspace ${xcode_workspace} -hideShellScriptEnvironment | sed -n \'/Schemes:/,+1 p\' | awk \'! /Schemes:\$/\'").trim()
 */

class XCodeBuilder implements Serializable {
    
    /**
     * Метод возвращает список доступных для сборки схем из .xcworkspace.
     * По умолчанию xcode workspace определятся из корневой сборочной директории по префиксу *(IOS).xcworkspace
     * 
     * @param script            Script, req - Контекст DSL
     * @param xcode_workspace   String, opt - Имя .xcworkspace файла, default: *(IOS).xcworkspace.
     */
    static void listScheme(Map args = [:]) {
        String xcode_workspace              = args.xcode_workspace ?: args.script.sh (returnStdout: true, script: 'find . -iname "*(IOS).xcworkspace"  | sed \'s/\\ /\\\\ /g\' | sed \'s/(/\\\\(/g\' | sed \'s/)/\\\\)/g\'').trim()
        
        args.script.println ">>> Parsing XCode schemes. Workspace:${xcode_workspace}"
        args.script.sh "xcodebuild -list -workspace ${xcode_workspace} -hideShellScriptEnvironment"
    }

    /**
     * Метод собирает билд из указанной схемы
     * 
     * @param script            String, req - Контекст DSL
     * @param xcode_scheme      String, opt - Имя схемы для сборки.
     * @param xcode_workspace   String, opt - Имя .xcworkspace файла, default: *(IOS).xcworkspace.
     */
    static void buildScheme(Map args = [:]) {
        String xcode_workspace              = args.xcode_workspace ?: args.script.sh (returnStdout: true, script: 'find . -iname "*(IOS).xcworkspace"  | sed \'s/\\ /\\\\ /g\' | sed \'s/(/\\\\(/g\' | sed \'s/)/\\\\)/g\'').trim()
        String xcode_scheme                 = args.xcode_scheme ?: args.script.sh (returnStdout: true, script: "xcodebuild -list -workspace ${xcode_workspace} -hideShellScriptEnvironment | sed -n \'/Schemes:/,+1 p\' | awk \'! /Schemes:\$/\'").trim()

        args.script.println ">>> Building XCode. Scheme:${xcode_scheme}, workspace:${xcode_workspace}"
        args.script.sh """xcodebuild build \
                            -scheme ${xcode_scheme} \
                            -workspace ${xcode_workspace} \
                            -sdk iphoneos -destination generic/platform=iOS \
                            -hideShellScriptEnvironment
                        """
    }

    /**
     * Метод для архивация приложения(.xcarchive).
     * 
     * @param script                        Script, req - Контекст DSL
     * @param xcode_scheme                  String, opt - Имя схемы для сборки.
     * @param xcode_workspace               String, opt - Имя .xcworkspace файла, default: *(IOS).xcworkspace.
     * @param xcode_output_appstore_path    String, opt - Путь до xcode output директории app-store окружения, default ./_build/_xcode/_output/_app-store.
     */
    static void generateArchive(Map args = [:]) {
        String xcode_workspace              = args.xcode_workspace ?: args.script.sh (returnStdout: true, script: 'find . -iname "*(IOS).xcworkspace"  | sed \'s/\\ /\\\\ /g\' | sed \'s/(/\\\\(/g\' | sed \'s/)/\\\\)/g\'').trim()
        String xcode_scheme                 = args.xcode_scheme ?: args.script.sh (returnStdout: true, script: "xcodebuild -list -workspace ${xcode_workspace} -hideShellScriptEnvironment | sed -n \'/Schemes:/,+1 p\' | awk \'! /Schemes:\$/\'").trim()
        String xcode_output_appstore_path   = args.xcode_output_appstore_path ?: './_build/_xcode/_output/_app-store'
        
        args.script.println ">>> Create XCode archive. Scheme:${xcode_scheme}, workspace:${xcode_workspace}, output:${xcode_output_appstore_path}/${xcode_scheme}.xcarchive"
        args.script.sh """xcodebuild archive \
                            -scheme ${xcode_scheme} \
                            -workspace ${xcode_workspace} \
                            -sdk iphoneos -destination generic/platform=iOS \
                            -archivePath ${xcode_output_appstore_path}/${xcode_scheme}.xcarchive \
                            -hideShellScriptEnvironment
                        """
    }

    /**
     * Генерация переменных для экспорта, конфигурирование info.plist файла.
     * По умолчанию редактируется info.plist из архива по пути ./_build/_xcode/_output/_app-store/${xcode_scheme}.xcarchive/info.plist.
     * 
     * @param script                            Script, req - Контекст DSL
     * @param xcode_cfbundleversion             String, opt - Номер билда(default: 'date "+%Y%m%d%H%M%S"')
     * @param xcode_infoplist_appstore_path     String, opt - Путь до info.plist файла, default: ./_build/_xcode/_output/_app-store/${xcode_scheme}.xcarchive/info.plist.
     */
    static void setInfoPlist(Map args = [:]) {
        String xcode_workspace                  = args.xcode_workspace ?: args.script.sh (returnStdout: true, script: 'find . -iname "*(IOS).xcworkspace"  | sed \'s/\\ /\\\\ /g\' | sed \'s/(/\\\\(/g\' | sed \'s/)/\\\\)/g\'').trim()
        String xcode_scheme                     = args.xcode_scheme ?: args.script.sh (returnStdout: true, script: "xcodebuild -list -workspace ${xcode_workspace} -hideShellScriptEnvironment | sed -n \'/Schemes:/,+1 p\' | awk \'! /Schemes:\$/\'").trim()
        String xcode_cfbundleversion            = args.xcode_cfbundleversion ?: args.script.sh (returnStdout: true, script: 'date "+%Y%m%d%H%M%S"').trim()
        String xcode_output_appstore_path       = args.xcode_output_appstore_path ?: './_build/_xcode/_output/_app-store'
        String xcode_infoplist_appstore_path    = args.xcode_infoplist_appstore_path ?: "${xcode_output_appstore_path}/${xcode_scheme}.xcarchive/info.plist"
        
        args.script.println ">>> Editing Info.plist, setting CFBundleVersion. Info.plist path:${xcode_infoplist_appstore_path}, CFBundleVersion:${xcode_cfbundleversion}"
        args.script.sh """/usr/libexec/PlistBuddy -c \
                        "Add ApplicationProperties:CFBundleVersion string ${xcode_cfbundleversion}" \
                        "${xcode_infoplist_appstore_path}" 
                        """
        args.script.sh "cat ${xcode_infoplist_appstore_path}"
    }

    /**
     * Экспорт архива в TestFlight.
     * 
     * @param script                            Script, req - Контекст DSL
     * @param xcode_output_appstore_path        String, opt - Путь до xcode output директории app-store окружения, default ./_build/_xcode/_output/_app-store.
     * @param xcode_exportplist_appstore_path   String, opt - Путь до ExportOptions.plist файла, app-store окружение.
     */
    static void exportArchive(Map args = [:]) {    
        String xcode_workspace                  = args.xcode_workspace ?: args.script.sh (returnStdout: true, script: 'find . -iname "*(IOS).xcworkspace"  | sed \'s/\\ /\\\\ /g\' | sed \'s/(/\\\\(/g\' | sed \'s/)/\\\\)/g\'').trim()
        String xcode_scheme                     = args.xcode_scheme ?: args.script.sh (returnStdout: true, script: "xcodebuild -list -workspace ${xcode_workspace} -hideShellScriptEnvironment | sed -n \'/Schemes:/,+1 p\' | awk \'! /Schemes:\$/\'").trim()        
        String xcode_output_appstore_path       = args.xcode_output_appstore_path ?: './_build/_xcode/_output/_app-store'
        String xcode_infoplist_appstore_path    = args.xcode_infoplist_appstore_path ?: "${xcode_output_appstore_path}/${xcode_scheme}.xcarchive/info.plist"
        String xcode_exportplist_appstore_path  = args.xcode_exportplist_appstore_path ?: "${xcode_output_appstore_path}/ExportOptions-AppStore.plist"        

        args.script.sh """xcodebuild -exportArchive \
                            -archivePath "${xcode_output_appstore_path}/${xcode_scheme}.xcarchive" \
                            -exportOptionsPlist "${xcode_exportplist_appstore_path}" \
                            -exportPath "${xcode_output_appstore_path}" \
                            -hideShellScriptEnvironment
                        """

    }

}
