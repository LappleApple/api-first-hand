package de.zalando.apifirst.generators

import de.zalando.apifirst.Application._
import de.zalando.apifirst.Domain._
import de.zalando.apifirst.ScalaName._
import de.zalando.apifirst.generators.DenotationNames.DenotationTable
import PlayScalaControllerAnalyzer._
import de.zalando.apifirst.StringUtil
import org.slf4j.LoggerFactory
import ControllersCommons._

import scala.collection.Iterable

/**
 * @author slasch
 * @since 16.11.2015.
 */

class ScalaGenerator(
    val strictModel: StrictModel,
    providedWriterFactories: Set[String] = Set.empty,
    customTemplateLocation: Option[String] = None
) {

  val denotationTable = AstScalaPlayEnricher(strictModel, providedWriterFactories)

  val StrictModel(modelCalls, modelTypes, modelParameters, discriminators, _, overridenPackageName, stateTransitions, securityDefinitions) = strictModel

  val testsTemplateName = "play_scala_test"
  val validatorsTemplateName = "play_validation"
  val generatorsTemplateName = "generators"
  val modelTemplateName = "model"
  val controllersTemplateName = "play_scala_controller"
  val controllerBaseTemplateName = "play_scala_controller_base"
  val marschallersTemplateName = "play_scala_response_writers"
  val securityTemplateName = "play_scala_controller_security"
  val securityExtractorsTemplateName = "play_scala_security_extractors"
  val formParsersTemplateName = "play_scala_form_parser"

  def generateBase: (String, String, String) => Seq[String] = (fileName, packageName, currentController) => Seq(
    generateModel(fileName, packageName),
    playValidators(fileName, packageName),
    playScalaSecurity(fileName, packageName),
    playScalaControllerBases(fileName, packageName),
    playScalaFormParsers(fileName, packageName)
  )

  def generateTest: (String, String, String) => Seq[String] = (fileName, packageName, currentController) => Seq(
    generateGenerators(fileName, packageName),
    playScalaTests(fileName, packageName)
  )

  def generateMarshallers: (String, String, String) => Seq[String] = (fileName, packageName, currentController) => Seq(
    playScalaMarshallers(fileName, packageName)
  )

  def generateExtractors: (String, String, String) => Seq[String] = (fileName, packageName, currentController) => Seq(
    playScalaSecurityExtractors(fileName, packageName)
  )

  def generateControllers: (String, String, String) => Seq[String] = (fileName, packageName, currentController) => Seq(
    playScalaControllers(fileName, packageName, currentController)
  )

  def generateModel(fileName: String, packageName: String): String =
    if (modelTypes.values.forall(_.isInstanceOf[PrimitiveType])) ""
    else apply(fileName, packageName, modelTemplateName)

  def generateGenerators(fileName: String, packageName: String): String =
    if (modelTypes.isEmpty) ""
    else apply(fileName, packageName, generatorsTemplateName)

  def playValidators(fileName: String, packageName: String): String =
    if (modelCalls.map(_.handler.parameters.size).sum == 0) ""
    else apply(fileName, packageName, validatorsTemplateName)

  def playScalaTests(fileName: String, packageName: String): String =
    if (modelCalls.map(_.handler.parameters.size).sum == 0) ""
    else apply(fileName, packageName, testsTemplateName)

  def playScalaControllers(fileName: String, packageName: String, currentController: String): String =
    if (modelCalls.isEmpty) ""
    else apply(fileName, packageName, controllersTemplateName, currentController)

  def playScalaControllerBases(fileName: String, packageName: String): String =
    if (modelCalls.isEmpty) ""
    else apply(fileName, packageName, controllerBaseTemplateName)

  def playScalaFormParsers(fileName: String, packageName: String): String =
    if (modelCalls.isEmpty) ""
    else apply(fileName, packageName, formParsersTemplateName)

  def playScalaMarshallers(fileName: String, packageName: String): String =
    if (modelCalls.isEmpty) ""
    else apply(fileName, packageName, marschallersTemplateName)

  def playScalaSecurityExtractors(fileName: String, packageName: String): String =
    if (modelCalls.isEmpty) ""
    else apply(fileName, packageName, securityExtractorsTemplateName)

  def playScalaSecurity(fileName: String, packageName: String): String =
    if (securityDefinitions.isEmpty) ""
    else apply(fileName, packageName, securityTemplateName)

  private def apply(fileName: String, packageName: String, templateName: String,
    currentController: String = ""): String = {
    nonEmptyTemplate(fileName, packageName, templateName, currentController)
  }

  //noinspection ScalaStyle
  private def nonEmptyTemplate(fileName: String, packageName: String, templateName: String, currentController: String): String = {

    assert(packageName.contains('-') == packageName.contains('`'), packageName)

    val validations = ReShaper.filterByType("validators", denotationTable)
    val validationsByType = ReShaper.groupByType(validations.toSeq).toMap

    val bindings = ReShaper.filterByType("bindings", denotationTable)
    val grouppedBindings = ReShaper.groupByType(bindings.toSeq.distinct)
    val sortedBindings = grouppedBindings.map {
      case (x, y: Seq[Map[String, Any] @unchecked]) =>
        val sorted = y.sortWith { (a, b) =>
          (a.get("dependencies"), b.get("dependencies")) match {
            case (Some(aa: Int), Some(bb: Int)) => aa < bb
            case _ if b("format").toString.contains("[") => true
            case _ => false
          }
        }
        x -> sorted
    }

    val bindingsByType = sortedBindings.toMap
    val modelBindings = bindingsByType.flatMap {
      case (k, b) =>
        b.map(_("format"))
    }.toSeq.distinct.map { b => Map("full_name" -> b) }

    val forms = ReShaper.filterByType("form_data", denotationTable)

    val marshallers = ReShaper.filterByType("marshallers", denotationTable)
    val grouppedMarshallers = ReShaper.groupByType(marshallers.toSeq).toMap

    val unmarshallers = ReShaper.filterByType("unmarshallers", denotationTable)
    val grouppedunMarshallers = ReShaper.groupByType(unmarshallers.toSeq).toMap

    val jsonWritables = ReShaper.filterByType("json_writables", denotationTable)
    val grouppedWritables = ReShaper.groupByType(jsonWritables.toSeq).toMap

    val jsonReadables = ReShaper.filterByType("json_readables", denotationTable)
    val grouppedReadables = ReShaper.groupByType(jsonReadables.toSeq).toMap

    val securityExtractors = ReShaper.filterByType("security_extractors", denotationTable)
    val extractors = ReShaper.groupByType(securityExtractors.toSeq).toMap

    val controllerLines = currentController.split("\n")
    val codeParts = collectImplementations(controllerLines, sof, eof)
    val constructors = collectImplementations(controllerLines, csof, ceof)
    val injections = collectImplementations(controllerLines, isof, ieof)

    val constructorCode = cleanFromComments(constructors, csof, ceof)
    val injectedCode = cleanFromComments(injections, isof, ieof)

    val unmanagedParts = analyzeController(modelCalls, codeParts)

    val deadCode = codeParts.filterNot { cp =>
      unmanagedParts.keys.map(asMarker).exists(_ == cp._1)
    }.map {
      case (k, v) =>
        k -> v.mkString("\n")
    }

    val userImports = unmanagedImports(currentController, modelTypes)

    val pckg = overridenPackageName.getOrElse(packageName)

    val formParsersRequired = forms.exists(_("form_parameters").asInstanceOf[Seq[_]].nonEmpty)

    val packages = Map(
      "main_package" -> pckg,
      "main_package_prefix" -> pckg.split('.').init.mkString("."),
      "main_package_suffix" -> pckg.split('.').last,
      "spec_name" -> escape(StringUtil.capitalize("\\.", fileName) + "Spec"),
      "form_parsers_required" -> formParsersRequired
    )

    val controllersList = PlayScalaControllersGenerator.
      controllers(modelCalls, unmanagedParts, pckg, deadCode, constructorCode, injectedCode)(denotationTable)

    val stdImports = standardImports(modelTypes).map(i => Map("name" -> i))

    val controllersMap = Map(
      "controllers" -> controllersList,
      "controller_imports" -> (controllerImports.map(i => Map("name" -> i)) ++ stdImports),
      "unmanaged_imports" -> userImports.map(i => Map("name" -> i))
    )

    val singlePackage: Map[String, Iterable[Any]] = Map(
      "classes" -> ReShaper.filterByType("classes", denotationTable),
      "aliases" -> ReShaper.filterByType("aliases", denotationTable),
      "traits" -> ReShaper.filterByType("traits", denotationTable),
      "enums" -> ReShaper.filterByType("enums", denotationTable),
      "test_data_classes" -> ReShaper.filterByType("test_data_classes", denotationTable),
      "test_data_aliases" -> ReShaper.filterByType("test_data_aliases", denotationTable),
      "tests" -> ReShaper.filterByType("tests", denotationTable),
      "marshallers" -> grouppedMarshallers,
      "unmarshallers" -> grouppedunMarshallers,
      "json_writables" -> grouppedWritables,
      "json_readables" -> grouppedReadables,
      "security_extractors" -> extractors,
      "bindings" -> bindingsByType,
      "forms" -> forms,
      "model_bindings" -> modelBindings
    )

    val rawAllPackages = singlePackage ++ validationsByType ++ controllersMap
    val allPackages = enrichWithStructuralInfo(rawAllPackages)

    renderTemplate(packages, templateName, allPackages)
  }

  private def cleanFromComments(constructors: Map[String, Seq[String]], start: String, end: String) = {
    constructors map {
      case (k, v) =>
        k -> v.filterNot(l => l.trim.startsWith(start) || l.trim.startsWith(end)).mkString("\n")
    } withDefaultValue ""
  }

  def renderTemplate(map: Map[String, Any], templateName: String,
    allPackages: Map[String, Any]): String = {
    import de.zalando.beard.renderer._
    val templateSuffix = ".mustache"
    val templatePrefix = "/"
    val loader = customTemplateLocation flatMap { f =>
      val targetTemplate = new java.io.File(f + templatePrefix + templateName + templateSuffix)
      if (targetTemplate.canRead) Some(new FileTemplateLoader(f, templateSuffix)) else None
    } getOrElse {
      new ClasspathTemplateLoader(templatePrefix, templateSuffix)
    }

    val templateCompiler = new CustomizableTemplateCompiler(loader)
    val template = templateCompiler.compile(TemplateName(templateName)).get
    val renderer = new BeardTemplateRenderer(templateCompiler)

    renderer.render(
      template,
      StringWriterRenderResult(),
      map ++ allPackages,
      None
    ).toString
  }

  def enrichWithStructuralInfo(rawAllPackages: Map[String, Iterable[Any]]): Map[String, Any] = {
    val imports = KeyCollector.collect("imports")(rawAllPackages)
    val importMaps = imports.distinct map { i => Map("name" -> i) }

    val bind_imports = KeyCollector.collect("binding_imports")(rawAllPackages)
    val bind_importMaps = bind_imports.distinct map { i => Map("name" -> i) }

    val allPackages = LastListElementMarks.set(rawAllPackages) ++
      neededParts(imports) + ("imports" -> importMaps) + ("binding_imports" -> bind_importMaps)
    allPackages
  }

  private val partsMapping = Map(
    "lists_part" -> "ArrayWrapper",
    "maps_part" -> "Map",
    "date_part" -> "LocalDate",
    "date_time_part" -> "DateTime",
    "binary_string_part" -> "BinaryString",
    "base64_string_part" -> "Base64String",
    "file_part" -> "File",
    "uuid_part" -> "UUID"
  )

  private def neededParts(imports: Seq[String]): Map[String, Boolean] = partsMapping map {
    case (k, v) => k -> imports.exists(_.contains(v))

  }
}

object PlayScalaControllersGenerator {

  lazy val log = LoggerFactory.getLogger(this.getClass)

  val baseControllersSuffix = "Base"
  val securityTraitSuffix = "Security"

  def controllers(allCalls: Seq[ApiCall], unmanagedParts: Map[ApiCall, UnmanagedPart], packageName: String,
    deadCode: Map[String, String], constructorCode: Map[String, String], injectedCode: Map[String, String])(table: DenotationTable): Iterable[Map[String, Object]] = {
    allCalls groupBy { c =>
      (c.handler.packageName, c.handler.controller)
    } map {
      case (controller, calls) =>
        val methods = calls map {
          singleMethod(unmanagedParts, table)
        }
        if (packageName != controller._1) {
          log.warn(s"Ignoring package part of the handler name '${controller._1}', using '$packageName' instead. \n\t" +
            "Current plugin version only supports single package definition per specification.\n\t" +
            "Play's route files will fail to compile.")
        }
        val securityTrait = calls.find(_.security.nonEmpty).map(_ => escape(controller._2 + securityTraitSuffix))
        val deadCodeParts = deadCode.toSeq.map {
          case (k, v) =>
            Map("name" -> k, "code" -> v)
        }
        val controllerName = escape(controller._2)
        Map(
          "effective_package" -> packageName,
          "controller" -> controllerName,
          "base" -> escape(controller._2 + baseControllersSuffix),
          "methods" -> methods,
          "security_trait" -> securityTrait,
          "dead_code" -> deadCodeParts,
          "start_comment" -> (csof + controllerName),
          "end_comment" -> (ceof + controllerName),
          "constructor_code" -> constructorCode(controller._2),
          "inject_start_comment" -> (isof + controllerName),
          "inject_end_comment" -> (ieof + controllerName),
          "inject_code" -> injectedCode(controller._2)
        )
    }
  }

  def singleMethod(unmanagedParts: Map[ApiCall, UnmanagedPart], table: DenotationTable): ApiCall => Map[String, Any] =
    call => {
      val method = table(call.asReference)("controller")
      val methodWithCode = method + (
        "implementation" -> unmanagedParts.get(call).map(_.relevantCode.mkString("\n").trim).getOrElse("NotImplementedYet")
      )
      methodWithCode
    }
}
