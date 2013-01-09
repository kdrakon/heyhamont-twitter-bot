
package com.mrsjstudios.heyhamont

import twitter4j._
import sun.security.jca.GetInstance
import twitter4j.conf.ConfigurationBuilder
import twitter4j.auth.OAuthAuthorization
import scala.io.Source

/**
  * @author Sean Policarpio
  *
  */

class TwitterBot {

	// read in the OAuth parameters
	val configParameters = TwitterBot.readConfigFile("resources/twitterconfig")

	// configure the twitter stream
	val conf = (new ConfigurationBuilder)
		.setUser(configParameters("user"))
		.setOAuthConsumerKey(configParameters("consumerKey"))
		.setOAuthConsumerSecret(configParameters("consumerSecret"))
		.setOAuthAccessToken(configParameters("accessToken"))
		.setOAuthAccessTokenSecret(configParameters("accessTokenSecret"))
		.build

	// create a handle to the twitter bots status
	val twitterStatus = new TwitterFactory(conf).getInstance()

	// create a handle to the public twitter stream
	val twitterStream = new TwitterStreamFactory(conf).getInstance()

	// add the listener
	twitterStream.addListener(new StreamListener(twitterStatus))

	// start listening to the stream (filter)
	val filter = new FilterQuery
	filter.track(configParameters("filter").split(";"))
	twitterStream.filter(filter)

}

object TwitterBot {

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

class StreamListener(twitterStatus : Twitter) extends StatusListener {

	// the bot will simply perform a "retweet" of all found tweets
	def onStatus(status : Status) {
		// we only want those tweets that aren't directed between users
		if (status.getInReplyToUserId == -1) twitterStatus.retweetStatus(status.getId())
	}

	def onDeletionNotice(statusDeletionNotice : StatusDeletionNotice) {}
	def onTrackLimitationNotice(numberOfLimitedStatuses : Int) {}
	def onScrubGeo(userId : Long, upToStatusId : Long) {}
	def onStallWarning(warning : StallWarning) {}
	def onException(ex : Exception) { ex.printStackTrace() }

}

object TwitterBotExecutor extends App {

	// start the bot from main()
	val bot = new TwitterBot

}
