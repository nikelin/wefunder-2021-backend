package com.wefunder.pitchdeck.db

import doobie.ConnectionIO
import doobie.implicits._

package object schema {

  def setupSchema: ConnectionIO[Unit] = {
    val presentationSql =
      sql"""
        create table if not exists presentation(
          id bigserial not null,
          title text not null,
          author text not null,
          description text not null,
          primary key (id)
        );
    """

    val presentationPageSql =
      sql"""
        create table if not exists presentation_page(
            id bigserial not null,
            presentation_id bigint not null,
            page_num int,
            name text not null,
            size bigint not null,
            created_at timestamp not null,
            path text not null,
            unique (presentation_id, page_num),
            primary key (id),
            foreign key (presentation_id) references presentation(id) on delete cascade
        )     
       """
    for {
      _ <- presentationSql.update.run
      _ <- presentationPageSql.update.run
    } yield {}
  }
}
