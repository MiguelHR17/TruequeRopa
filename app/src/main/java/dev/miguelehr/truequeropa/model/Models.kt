package dev.miguelehr.truequeropa.model

enum class Size { XS, S, M, L, XL }
enum class Condition { NUEVO, USADO }
enum class Category { CAMISA, PANTALON, VESTIDO, CHAQUETA, ZAPATOS, ACCESORIO }

data class User(val id:String, val nombre:String, val correo:String, val roles:Set<String> = setOf("USER"), val tallasPreferidas:Set<Size> = setOf(Size.M))
data class Product(val id:String, val ownerId:String, val titulo:String, val descripcion:String, val talla:Size, val estado:Condition, val categoria:Category, val imageUrl:String)
data class TradeProposal(val id:String, val fromUserId:String, val toUserId:String, val offeredProductId:String, val requestedProductId:String, val status:ProposalStatus = ProposalStatus.PENDIENTE)
enum class ProposalStatus { PENDIENTE, ACEPTADA, RECHAZADA }
data class TradeHistoryItem(val id:String, val prendaOfrecidaId:String, val prendaRecibidaId:String, val fecha:String)
data class ReportMetrics(val usuariosActivos:Int, val truequesRealizados:Int, val categoriasTop:List<Pair<Category,Int>>, val usuariosTop:List<Pair<String,Int>>)