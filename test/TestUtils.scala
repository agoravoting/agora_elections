/**
 * This file is part of agora_elections.
 * Copyright (C) 2014-2016  Agora Voting SL <agora@agoravoting.com>

 * agora_elections is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.

 * agora_elections  is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with agora_elections.  If not, see <http://www.gnu.org/licenses/>.
**/
package test

import models._
import controllers.routes
import utils.Crypto

import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import play.api.db.slick.DB
import org.specs2.specification.AroundOutside
import play.api.Play.current
import com.typesafe.config.ConfigFactory
import play.Configuration
import play.api._
import play.api.libs.json.Json

object TestSettings {
  def getTestApp = FakeApplication(additionalConfiguration = testSettings)

  def testSettings = {
  	import collection.JavaConversions._

    val conf = ConfigFactory.parseFile(new java.io.File("conf/test.local.conf"))
    val map = conf.root().unwrapped()

    map.toMap
  }
}

trait TestContexts {

  object TestData {

    val config = Json.parse("""
{
  "id": 1,
  "title": "Votación de candidatos",
  "description": "Selecciona los documentos politico, etico y organizativo con los que Podemos",
  "director": "test-auth1.agoravoting.com",
  "authorities": ["test-auth2.agoravoting.com"],
  "layout": "pcandidates-election",
  "virtual": false,
  "tally_allowed": true,
  "presentation": {
    "theme": "foo",
    "urls": [
      {
        "title": "",
        "url": "http://www.google.com"
      }
    ],
    "theme_css": "whatever"
  },
  "end_date": "2013-12-09T18:17:14.457000",
  "start_date": "2013-12-06T18:17:14.457000",
  "questions": [
      {
          "description": "",
          "layout": "pcandidates-election",
          "max": 1,
          "min": 0,
          "num_winners": 1,
          "title": "Secretaria General",
          "tally_type": "plurality-at-large",
          "answer_total_votes_percentage": "over-total-valid-votes",
          "extra_options": {
              "shuffle_categories": true,
              "shuffle_all_options": true,
              "shuffle_category_list": []
          },
          "answers": [
              {
                  "id": 0,
                  "category": "Equipo de Enfermeras",
                  "details": "",
                  "sort_order": 1,
                  "urls": [
                    {
                      "title": "",
                      "url": "http://www.google.com"
                    }
                  ],
                  "text": "Fulanita de tal"
              }
          ]
      }
  ]
}
""")
  }

  def getAuth(userId: String, objType: String, objId: Long, perm: String) = {
    val authSecret = Play.current.configuration.getString("booth.auth.secret").get
    val time = (new java.util.Date().getTime / 1000)
    val head = s"$userId:$objType:$objId:$perm:$time"

    "khmac:///sha-256;" + Crypto.hmac(authSecret, head) + "/" + head
  }

  abstract class AppWithDbData(app: FakeApplication = TestSettings.getTestApp) extends WithApplication(app) {
    override def around[T: org.specs2.execute.AsResult](t: => T) = super.around {
      prepareDbWithData()
      org.specs2.execute.AsResult(t)
    }

    def prepareDbWithData() = {
      import scala.slick.jdbc.{GetResult, StaticQuery => Q}

      DB.withSession { implicit session =>
        Q.updateNA("TRUNCATE ELECTION").list
        Q.updateNA("TRUNCATE VOTE").list
      }
    }
  }
}
