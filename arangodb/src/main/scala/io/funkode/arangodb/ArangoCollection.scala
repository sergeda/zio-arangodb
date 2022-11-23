package io.funkode.arangodb

import zio.*

import model.*
import protocol.*

class ArangoCollection[Encoder[_], Decoder[_]](databaseName: DatabaseName, collectionName: CollectionName)(
    using arangoClient: ArangoClient[Encoder, Decoder]
):

  import ArangoMessage.*

  val database: DatabaseName = databaseName
  val name: CollectionName = collectionName
  val path = ApiCollectionPath.addPart(name.unwrap)

  def info(using Decoder[CollectionInfo]): AIO[CollectionInfo] =
    GET(database, path).execute

  def checksum(withRevisions: Boolean = false, withData: Boolean = false)(using
      Decoder[CollectionChecksum]
  ): AIO[CollectionChecksum] =
    GET(
      database,
      path.addPart("checksum"),
      Map(
        "withRevisions" -> withRevisions.toString,
        "withData" -> withData.toString
      )
    ).execute

  def create(setup: CollectionCreate => CollectionCreate = identity)(using
      Encoder[CollectionCreate],
      Decoder[CollectionInfo]
  ): AIO[CollectionInfo] =
    val options = setup(CollectionCreate(name))
    POST(database, ApiCollectionPath, options.parameters).withBody(options).execute

  def createIfNotExist(setup: CollectionCreate => CollectionCreate = identity)(using
      Encoder[CollectionCreate],
      Decoder[CollectionInfo]
  ): AIO[ArangoCollection[Encoder, Decoder]] =
    create(setup).map(_ => this).ifConflict(ZIO.succeed(this))

  def drop(isSystem: Boolean = false)(using D: Decoder[DeleteResult]): AIO[DeleteResult] =
    DELETE(database, path).execute

/*
  def revision(): F[ArangoResponse[CollectionRevision]]

  def properties(): F[ArangoResponse[CollectionProperties]]

  def update(waitForSync: Option[Boolean] = None,
    schema: Option[CollectionSchema] = None): F[ArangoResponse[CollectionProperties]]

  def truncate(waitForSync: Boolean = false, compact: Boolean = true): F[ArangoResponse[CollectionInfo]]

  def rename(newName: CollectionName): F[ArangoResponse[CollectionInfo]]

  def documents: ArangoDocuments[Encoder, Decoder] =
    new ArangoDocuments.Impl[Encoder, Decoder](databaseName, collectionName)(using arangoClient)

  def document(key: DocumentKey): ArangoDocument[Encoder, Decoder] =
    new ArangoDocument.Impl[Encoder, Decoder](databaseName, DocumentHandle(this.name, documentKey))(
      using arangoClient
    )

  def indexes: ArangoIndexes[F]

  def index(id: String): ArangoIndex[F]

  def all: ArangoQuery[F, VObject]
 */

object ArangoCollection:

  extension [R, Enc[_], Dec[_]](colService: ZIO[R, ArangoError, ArangoCollection[Enc, Dec]])
    def info(using Dec[CollectionInfo]): ZIO[R, ArangoError, CollectionInfo] =
      colService.flatMap(_.info)

    def create(setup: CollectionCreate => CollectionCreate = identity)(using
        Enc[CollectionCreate],
        Dec[CollectionInfo]
    ): ZIO[R, ArangoError, CollectionInfo] =
      colService.flatMap(_.create(setup))

    def drop(isSystem: Boolean = false)(using D: Dec[DeleteResult]): ZIO[R, ArangoError, DeleteResult] =
      colService.flatMap(_.drop(isSystem))
