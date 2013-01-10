
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
class TwitterStreamBot(configParameters : Map[ String, String ]) {

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
	twitterStream.addListener(new StreamListener(conf, configParameters("tweetBuffer").toInt))

	// start listening to the stream (filter)
	val filter = new FilterQuery
	filter.track(configParameters("filter").split(";"))
	twitterStream.filter(filter)

}

/**
  * The Twitter stream handler
  */
class StreamListener(conf : Configuration, tweetBuffer : Int) extends StatusListener {

	// a mutable collection of tweets collected over time
	var tweetCollection : MutableList[ Status ] = new MutableList

	def onStatus(status : Status) {
		// we only want those tweets that aren't directed between users
		if (status.getInReplyToUserId == -1 && status.getInReplyToStatusId == -1) {
			tweetCollection += status
			Console.println("Added status to collection: " + status.getId() + "[" + tweetCollection.length + "]")
		}

		// send the sorted list (based on retweets) to the retweet bot thread.
		if (tweetCollection.length >= tweetBuffer) {
//			new Thread(new TwitterRetweetBot(conf,
//				tweetCollection.sortWith((a : Status, b : Status) => a.getRetweetCount() > b.getRetweetCount()).toList))
//				.start()
			new Thread(new TwitterRetweetBot(conf,
				tweetCollection.toList))
				.start()			
			tweetCollection.clear // clear the buffer
		}

	}

	// not implemented...
	def onDeletionNotice(statusDeletionNotice : StatusDeletionNotice) {}
	def onTrackLimitationNotice(numberOfLimitedStatuses : Int) {}
	def onScrubGeo(userId : Long, upToStatusId : Long) {}
	def onStallWarning(warning : StallWarning) {}
	def onException(ex : Exception) { /*ex.printStackTrace()*/ }

}

/**
  * A thread responsible with the retweeting task
  */
class TwitterRetweetBot(conf : Configuration, tweetCollection : List[ Status ]) extends Runnable {

	// create a handle to the twitter bots status
	val botStatus = new TwitterFactory(conf).getInstance()

	/*
	 * Retweet each tweet with a 2s interval
	 */
	def run() {
		Console.println("Retweeting collection now...")
		tweetCollection.foreach((s : Status) => { botStatus.retweetStatus(s.getId()); Thread.sleep(2000) })
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

	// start the bot from main()
	//	val retweetBot = new TwitterRetweetBot(configParameters).start
	val streamBot = new TwitterStreamBot(configParameters)

}
