
package com.mrsjstudios.heyhamont

import twitter4j._
import sun.security.jca.GetInstance
import twitter4j.conf.ConfigurationBuilder
import twitter4j.auth.OAuthAuthorization
import scala.io.Source
import scala.collection.mutable.MutableList
import scala.actors.Actor
import twitter4j.conf.Configuration

/**
  * A Twitter bot implemented using the Twitter4J API. Made specifically for the @heyhamont twitter account.
  * @author Sean Policarpio
  *
  */

/**
  * The main TwitterBot implementation.
  */
class TwitterStreamBot(configParameters : Map[ String, String ], retweetBot : Actor) {

	// create the configuration
	val conf = (new ConfigurationBuilder)
		.setUser(configParameters("user"))
		.setOAuthConsumerKey(configParameters("consumerKey"))
		.setOAuthConsumerSecret(configParameters("consumerSecret"))
		.setOAuthAccessToken(configParameters("accessToken"))
		.setOAuthAccessTokenSecret(configParameters("accessTokenSecret"))
		.build

	// create a handle to the public twitter stream
	val twitterStream = new TwitterStreamFactory(conf).getInstance()

	// add the listener
	twitterStream.addListener(new StreamListener(configParameters("tweetBuffer").toInt, retweetBot))

	// start listening to the stream (filter)
	val filter = new FilterQuery
	filter.track(configParameters("filter").split(";"))
	twitterStream.filter(filter)

}

/**
  * A case class that wraps the collection of tweets to retweet.
  * Used specifically for the TwitterRetweetBot actor.
  */
case class TweetsToRetweet(tweetColl : List[ Status ])

/**
  * The Twitter stream handler
  */
class StreamListener(tweetBuffer : Int, retweetBot : Actor) extends StatusListener {

	// a mutable collection of tweets collected over time
	var tweetCollection : MutableList[ Status ] = new MutableList

	def onStatus(status : Status) {
		// we only want those tweets that aren't directed between users
		if (status.getInReplyToUserId == -1 && status.getInReplyToStatusId == -1) {
			tweetCollection += status
		}

		// send the sorted list (based on retweets) to the retweet bot. clear it afterwards.
		if (tweetCollection.length >= tweetBuffer) {			
			retweetBot ! new TweetsToRetweet(tweetCollection.sortWith((a : Status, b : Status) => a.getRetweetCount() > b.getRetweetCount()).toList)
			tweetCollection.clear
		}

	}

	// not implemented...
	def onDeletionNotice(statusDeletionNotice : StatusDeletionNotice) {}
	def onTrackLimitationNotice(numberOfLimitedStatuses : Int) {}
	def onScrubGeo(userId : Long, upToStatusId : Long) {}
	def onStallWarning(warning : StallWarning) {}
	def onException(ex : Exception) { ex.printStackTrace() }

}

/**
  * An actor responsible with the retweeting task
  */
class TwitterRetweetBot(configParameters : Map[ String, String ]) extends Actor {

	// create the configuration
	val conf = (new ConfigurationBuilder)
		.setUser(configParameters("user"))
		.setOAuthConsumerKey(configParameters("consumerKey"))
		.setOAuthConsumerSecret(configParameters("consumerSecret"))
		.setOAuthAccessToken(configParameters("accessToken"))
		.setOAuthAccessTokenSecret(configParameters("accessTokenSecret"))
		.build

	// create a handle to the twitter bots status
	val botStatus = new TwitterFactory(conf).getInstance()

	def act() {
		while (true) {
			receive {
				case TweetsToRetweet(tweetColl) =>
					tweetColl.foreach((s : Status) => botStatus.retweetStatus(s.getId()))
			}
		}
	}

}

/**
 * Helper methods for bots
 */
object ConfigLoader {

	/**
	  * Read the local file which should contain mappings for config parameters
	  */
	def readConfigFile(filePath : String) : Map[ String, String ] = {
		val lines = Source.fromFile(filePath, "UTF-8").getLines
		val parameterMap = for (line <- lines) yield {
			val tuple = line.split("=")
			(tuple(0), tuple(1))
		}
		parameterMap.toMap
	}

}

object TwitterBotExecutor extends App {

	// read in the bot parameters
	val configParameters = ConfigLoader.readConfigFile("resources/twitterconfig")

	// start the bots from main()
	val retweetBot = new TwitterRetweetBot(configParameters)
	val streamBot = new TwitterStreamBot(configParameters, retweetBot)

}
