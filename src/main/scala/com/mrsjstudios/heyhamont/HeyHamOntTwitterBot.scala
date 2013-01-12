
package com.mrsjstudios.heyhamont

import twitter4j._
import twitter4j.conf.ConfigurationBuilder
import twitter4j.auth.OAuthAuthorization
import scala.io.Source
import scala.collection.mutable.MutableList
import twitter4j.conf.Configuration

/**
  * A Twitter bot implemented using the Twitter4J API. Made specifically for the @heyhamont twitter account.
  * @author Sean Policarpio  *
  *
  * The following is the main TwitterBot implementation.
  */
class HeyHamOntTwitterBot(configParameters : Map[ String, String ]) {

	// create the configuration
	val conf = ConfigLoader.createTwitterConfig(configParameters)

	// create a handle to the public twitter stream
	val twitterStream = new TwitterStreamFactory(conf).getInstance()

	// add the listener
	twitterStream.addListener(new StreamListener(configParameters("tweetBuffer").toInt))

	// create the analytics object to sort the top tweets
	val twitterAnalyser = new TopTweetAnalyser(conf, configParameters("analyserCycle").toInt, configParameters("topTweetsLength").toInt)

	// start listening to the stream (filter)
	val filter = new FilterQuery
	filter.track(configParameters("filter").split(";"))
	twitterStream.filter(filter)

	/**
	  * The Twitter stream handler
	  */
	private class StreamListener(tweetBuffer : Int) extends StatusListener {

		// a mutable collection of tweets collected over time
		var tweetCollection : MutableList[ Status ] = new MutableList

		def onStatus(status : Status) {
			/* we only want those tweets that aren't directed between users; 
			 * and aren't retweets already; and have more than 10 followers.
			 */
			if (status.getInReplyToUserId == -1 &&
				status.getInReplyToStatusId == -1 &&
				!status.isRetweet() &&
				status.getUser().getFollowersCount() >= 10) {
				tweetCollection += status
				Console.println("Added status to collection: " + status.getId() + "[" + tweetCollection.length + "]")
			}

			// send the tweets to a retweet bot thread. 
			// Also, append these tweets to the analytics object.
			if (tweetCollection.length >= tweetBuffer) {
				new Thread(new TwitterRetweetBot(tweetCollection.toList)).start()
				twitterAnalyser updateWith tweetCollection.toList
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
	private class TwitterRetweetBot(tweetCollection : List[ Status ]) extends Runnable {

		// create a handle to the twitter bots status
		val botStatus = new TwitterFactory(conf).getInstance()

		/*
		 * Retweet each tweet with a 30s interval.
		 * If a TwitterException is caught (or any other one), sleep for 60s
		 */
		def run() {
			Console.println("Retweeting collection now...")

			for (s <- tweetCollection) {
				try {
					botStatus.retweetStatus(s.getId())
					Console.println("TWEETING:" + s.getText())
					Thread.sleep(30000)
				} catch {
					case e : TwitterException => {
						Console.println("Twitter exception caught. Going to sleep for 60s. [" + s.getId() + "]")
						Thread.sleep(60000)
					}
				}

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

	/**
	  * Generates the Twitter config object
	  */
	def createTwitterConfig(configParameters : Map[ String, String ]) : Configuration = {
		(new ConfigurationBuilder)
			.setUser(configParameters("user"))
			.setOAuthConsumerKey(configParameters("consumerKey"))
			.setOAuthConsumerSecret(configParameters("consumerSecret"))
			.setOAuthAccessToken(configParameters("accessToken"))
			.setOAuthAccessTokenSecret(configParameters("accessTokenSecret"))
			.build
	}

}

/**
  * The executor (main method) of the Twitter bot
  */
object TwitterBotExecutor extends App {

	// read in the bot parameters
	val configParameters = ConfigLoader.readConfigFile("resources/twitterconfig")

	// start the bot from main()
	val heyhamontBot = new HeyHamOntTwitterBot(configParameters)

}
