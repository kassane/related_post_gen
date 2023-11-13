import upickle.*
import upickle.default.*
import os._
import scala.collection.mutable.*

case class Post(_id: String, title: String, tags: Array[String])
object Post {
  implicit val rw: ReadWriter[Post] = upickle.default.macroRW
}
case class RelatedPost(_id: String, tags: Array[String], related: Array[Post])
object RelatedPost {
  implicit val rw: ReadWriter[RelatedPost] = upickle.default.macroRW
}

object Main {
  val TopN = 5

  def main(args: Array[String]): Unit =
    val jsonContent = os.read(os.pwd / os.up / "posts.json")

    val posts = upickle.default.read[Array[Post]](jsonContent)

    val start = System.currentTimeMillis()

    val postsWithIndex = posts.zipWithIndex

    val postsCount = posts.length

    val tagMapTemp = Map[String, Buffer[Int]]()

    postsWithIndex.foreach { case (post, i) =>
      post.tags.foreach { tag =>
        tagMapTemp.get(tag) match {
          case Some(indexes) => indexes += i
          case None          => tagMapTemp(tag) = Buffer(i)
        }
      }
    }

    val tagMap = tagMapTemp.map { case (tag, indexes) =>
      tag -> indexes.toArray
    }

    val allRelatedPosts = postsWithIndex.map { case (post, i) =>
      val taggedPostCount = Array.fill(postsCount)(0)

      post.tags.foreach { tag =>
        tagMap(tag).foreach { index =>
          taggedPostCount(index) += 1
        }
      }

      taggedPostCount(i) = 0

      val top5 = Array.fill(TopN * 2)(0)
      var minTags = 0

      for (j <- taggedPostCount.indices) {
        val count = taggedPostCount(j)
        if (count > minTags) {
          var upperBound = (TopN - 2) * 2

          while (upperBound >= 0 && count > top5(upperBound)) {
            top5(upperBound + 2) = top5(upperBound)
            top5(upperBound + 3) = top5(upperBound + 1)
            upperBound -= 2
          }

          val insertPos = upperBound + 2
          top5(insertPos) = count
          top5(insertPos + 1) = j

          minTags = top5(TopN * 2 - 2)
        }
      }

      val topPosts = Array.tabulate(TopN)(i => posts(top5(i * 2 + 1)))

      RelatedPost(post._id, post.tags, topPosts)
    }

    val end = System.currentTimeMillis()
    println(s"Processing time (w/o IO): ${end - start}ms")

    val relatedJson = upickle.default.write(allRelatedPosts)
    os.write.over(os.pwd / os.up / "related_posts_scala.json", relatedJson)
}
