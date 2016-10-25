package split.petstore.api


    import de.zalando.play.controllers.ArrayWrapper
    import java.time.ZonedDateTime

    import de.zalando.play.controllers.PlayPathBindables


//noinspection ScalaStyle
package yaml {


    case class PetCategoryOpt(id: OrderPetId, name: OrderStatus) 
    case class Pet(name: String, tags: PetTags, photoUrls: PetPhotoUrls, id: OrderPetId, status: OrderStatus, category: PetCategory) 
    case class User(email: OrderStatus, username: OrderStatus, userStatus: OrderQuantity, lastName: OrderStatus, firstName: OrderStatus, id: OrderPetId, phone: OrderStatus, password: OrderStatus) 
    case class Order(shipDate: OrderShipDate, quantity: OrderQuantity, petId: OrderPetId, id: OrderPetId, complete: OrderComplete, status: OrderStatus) 


}

// should be defined after the package because of the https://issues.scala-lang.org/browse/SI-9922

//noinspection ScalaStyle
package object yaml {

    type UsersCreateWithListPostResponsesDefault = Null
    type OrderStatus = Option[String]
    type PetsFindByStatusGetStatusOpt = ArrayWrapper[String]
    type UsersCreateWithListPostBodyOpt = Seq[User]
    type OrderPetId = Option[Long]
    type PetsFindByStatusGetResponses200 = Seq[Pet]
    type PetsPostBody = Option[Pet]
    type OrderShipDate = Option[ZonedDateTime]
    type UsersUsernamePutBody = Option[User]
    type StoresOrderPostBody = Option[Order]
    type OrderComplete = Option[Boolean]
    type PetTags = Option[PetTagsOpt]
    type OrderQuantity = Option[Int]
    type PetPhotoUrls = Seq[String]
    type UsersCreateWithListPostBody = Option[UsersCreateWithListPostBodyOpt]
    type PetsFindByStatusGetStatus = Option[PetsFindByStatusGetStatusOpt]
    type PetCategory = Option[PetCategoryOpt]
    type PetTagsOpt = Seq[PetCategoryOpt]


import play.api.mvc.{QueryStringBindable, PathBindable}

    implicit val bindable_OptionStringQuery: QueryStringBindable[Option[String]] = PlayPathBindables.createOptionQueryBindable[String]
    implicit val bindable_ArrayWrapperStringQuery: QueryStringBindable[ArrayWrapper[String]] = PlayPathBindables.createArrayWrapperQueryBindable[String]("multi")
    implicit val bindable_OptionPetsFindByStatusGetStatusOptQuery: QueryStringBindable[Option[PetsFindByStatusGetStatusOpt]] = PlayPathBindables.createOptionQueryBindable[PetsFindByStatusGetStatusOpt]

}