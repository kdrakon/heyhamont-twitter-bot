/**
  *
  */
package com.mrsjstudios.heyhamont

import twitter4j._
import java.util.ArrayList
import twitter4j.conf.Configuration
import org.apache.commons.lang3.time.DateUtils
import java.util.Date
import scala.collection.immutable.List
import scala.collection.mutable.MutableList
import com.sun.org.apache.xalan.internal.xsltc.compiler.ForEach

/**
  * The classes/objects here mainly pertain to determining the most popular tweets collected from the bot.
  * It will sort all relevant tweets from within the present day.
  *
  * @author Sean Policarpio
  *
  */
class TopTweetAnalyser(conf : Configuration, cycle : Int, listSize : Int) {

	// The mutable list of tweets to analyse
	@volatile var tweets : MutableList[ Status ] = MutableList()
	// The volatile mutable list of top tweets
	@volatile var topTweets : MutableList[ Status ] = MutableList()

	/**
	  * Adds new tweets to the cache of already existing tweets.
	  * This method will ALSO filter out tweets from before the last 24hrs.
	  */
	def updateWith(newTweets : List[ Status ]) {

		val lastMidnight = DateUtils.setHours(new Date(), 0)
		tweets = tweets.filter((s : Status) => s.getCreatedAt().after(lastMidnight)) ++ newTweets

		// update the top tweets per cycle
		if (tweets.size % cycle == 0) {
			new Thread(new TopTweetsGenerator).start()
		}
	}

	/**
	  * For all the tweets in the cache, retrieve the current stats about them from Twitter.
	  * Then return the sorted list of popular tweets into the topTweets var.
	  */
	private class TopTweetsGenerator extends Runnable {
		
		def run(){

			// get a handle to the Twitter server
			val twitter = new TwitterFactory(conf).getInstance()
	
			// update the tweets to get current stats
			val updatedTweets : MutableList[ Status ] = for (oldTweet <- tweets) yield {
				try {
					Thread.sleep(1000)
					twitter.showStatus(oldTweet.getId())
				} catch {
					case e : TwitterException => {
						// I might be bombarding the Twitter server, so let's take a break
						Console.println("Failed at updating status. Reverting to old one.")
						Thread.sleep(30000)
						oldTweet
					}
				}
			}
	
			// sort the popular tweets and save
			topTweets = updatedTweets
				.filter((s : Status) => s.isFavorited())
				.sortBy((s : Status) => s.getRetweetCount()) ++
				updatedTweets
				.filter((s : Status) => !s.isFavorited())
				.sortBy((s : Status) => s.getRetweetCount())
				.take(listSize)
				
			Console.println("List of top tweets updated.")
			topTweets.foreach((s: Status) => Console.println(s.getText()))
			
		}
	}
}