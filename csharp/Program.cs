using System.Collections.Frozen;
using System.Diagnostics;
using System.Runtime.InteropServices;
using System.Text.Json;
using System.Text.Json.Serialization;


const int topN = 5;
var posts = JsonSerializer.Deserialize(File.ReadAllText(@"../posts.json"), MyJsonContext.Default.ListPost)!;

GetRelatedPosts(posts);
GC.Collect();

var sw = Stopwatch.StartNew();

var allRelatedPosts = GetRelatedPosts(posts);

sw.Stop();

Console.WriteLine($"Processing time (w/o IO): {sw.Elapsed.TotalMilliseconds}ms");

File.WriteAllText(@"../related_posts_csharp.json", JsonSerializer.Serialize(allRelatedPosts, MyJsonContext.Default.RelatedPostsArray));


static RelatedPosts[] GetRelatedPosts(List<Post> posts)
{
    var postsCount = posts.Count;

    // Create a dictionary to map tags to post IDs.
    var tagMapTemp = new Dictionary<string, LinkedList<int>>(100);

    for (var i = 0; i < postsCount; i++)
    {
        foreach (var tag in posts[i].Tags)
        {
            // single lookup
            ref var stack = ref CollectionsMarshal.GetValueRefOrAddDefault(tagMapTemp, tag, out _);
            stack ??= new LinkedList<int>();
            stack.AddLast(i);
        }
    }

    var tagMap = FrozenDictionary.ToFrozenDictionary(
        tagMapTemp,
        s => s.Key,
        s => s.Value.ToArray());

    // Create an array to store all of the related posts.
    var allRelatedPosts = new RelatedPosts[postsCount];
    var taggedPostCount = new byte[postsCount];

    // Iterate over all of the posts.
    for (var i = 0; i < postsCount; i++)
    {
        allRelatedPosts[i] = GetRelatedPosts(i, tagMap, taggedPostCount, posts);
    }

    static RelatedPosts GetRelatedPosts(int i, FrozenDictionary<string, int[]> tagMap, byte[] taggedPostCount, List<Post> posts)
    {
        // Reset the tagged post counts.
        ((Span<byte>)taggedPostCount).Fill(0);

        // Iterate over all of the tags for the current post.
        foreach (var tag in posts[i].Tags)
        {
            // Iterate over all of the related post IDs for the current tag.
            foreach (var otherPostIdx in tagMap[tag])
            {
                // Increment the tagged post count for the related post.
                taggedPostCount[otherPostIdx]++;
            }
        }

        taggedPostCount[i] = 0; // don't count self
        Span<(byte Count, int PostId)> top5 = stackalloc (byte, int)[5];
        byte minTags = 0;

        //  custom priority queue to find top N
        int p = 0;
        while ((uint)p < (uint)taggedPostCount.Length)
        {
            while ((uint)p < (uint)taggedPostCount.Length && taggedPostCount[p] <= minTags)
                p++;

            if ((uint)p < (uint)taggedPostCount.Length)
            {
                byte count = taggedPostCount[p];
                int upperBound = topN - 2;

                while (upperBound >= 0 && count > top5[upperBound].Count)
                {
                    top5[upperBound + 1] = top5[upperBound];
                    upperBound--;
                }

                top5[upperBound + 1] = (count, p);

                minTags = top5[topN - 1].Count;
            }

            p++;
        }

        var topPosts = new Post[topN];

        // Convert indexes back to Post references. skip even indexes
        for (int j = 0; j < 5; j++)
        {
            topPosts[j] = posts[top5[j].PostId];
        }

        return new RelatedPosts
        {
            Id = posts[i].Id,
            Tags = posts[i].Tags,
            Related = topPosts
        };
    }

    return allRelatedPosts;
}


public record struct Post
{
    [JsonPropertyName("_id")]
    public string Id { get; set; }

    [JsonPropertyName("title")]
    public string Title { get; set; }

    [JsonPropertyName("tags")]
    public string[] Tags { get; set; }
}

public record RelatedPosts
{
    [JsonPropertyName("_id")]
    public string Id { get; set; }

    [JsonPropertyName("tags")]
    public string[] Tags { get; set; }

    [JsonPropertyName("related")]
    public Post[] Related { get; set; }
}

[JsonSerializable(typeof(Post))]
[JsonSerializable(typeof(List<Post>))]
[JsonSerializable(typeof(RelatedPosts))]
[JsonSerializable(typeof(RelatedPosts[]))]
public partial class MyJsonContext : JsonSerializerContext { }
