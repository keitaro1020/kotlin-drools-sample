package sample

import org.drools.decisiontable.InputType
import org.drools.decisiontable.SpreadsheetCompiler
import org.kie.api.KieServices
import org.kie.api.builder.Message
import org.kie.api.runtime.KieSession
import java.nio.file.Paths

fun main(args: Array<String>) {

    val filepath = if(args.isNotEmpty()) {
        args[0]
    } else {
        throw IllegalArgumentException("ルールファイルが指定されていません")
    }

    val apples = listOf(
            Apple(size = 1),
            Apple(size = 5),
            Apple(size = 10),
            Apple(size = 30),
            Apple(size = 70),
            Apple(size = 120)
    )

    val kieSession = getKieSession(filepath)
    kieSession?.let {

        apples.forEach { apple ->
            it.insert(apple)
            it.fireAllRules()

            println("Size: ${apple.size} -> Rank: ${apple.rank}")
        }

        it.dispose()
    }
}

fun getKieSession(filepath: String) : KieSession? {

    val kieServices = KieServices.Factory.get()
    val kieFileSystem = kieServices.newKieFileSystem()

    val file = Paths.get(filepath).toFile()

    // (debug) xlsをdrl形式で表示する
    file.inputStream().use {
        val sc = SpreadsheetCompiler()
        println(sc.compile(it, InputType.XLS))
    }

    var kieSession: KieSession? = null

    file.inputStream().use {
        // inputStreamからの読み込み
        kieFileSystem.write(
                "src/main/resources/rules.xls",
                kieServices.resources.newInputStreamResource(it)
        )
        val kieBuilder = kieServices.newKieBuilder(kieFileSystem).buildAll()

        // ルールファイル記述内容のチェック
        val results = kieBuilder.results
        if (results.hasMessages(Message.Level.ERROR)) {
            println(results.toString())
            throw IllegalStateException(">>>ルールファイルの記述に問題があります。")
        }

        kieSession = kieServices
                .newKieContainer(kieServices.repository.defaultReleaseId)
                .newKieSession()
    }

    return kieSession
}

data class Apple(val size: Long, var rank: String = "")