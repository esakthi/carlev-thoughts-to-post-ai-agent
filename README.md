# carlev-thoughts-to-post-ai-agent
An Concept to thoughts to Idea to social media post


The repo has 3 sub projects, Overall the application contains one UI, spring boot service and python ai agent service. The functionality of the application is to get the thoughts / idea from user, store it in mongodb and enrich the content and image representation using LLM, review the generated content with user the post enriched content and image to LinkedIn and other social media.

1. Front end: thoughts-to-post-angular-ui -> Angular UI get's user thoughts / idea to enrich and post to social media. User selects the social media platform like LinkedIn. 

2. Java spring boot backend service: thoughts-to-post-api-service -> This microservice acts as backend to Angular UI service, has rest API's process and store user input messages

3. AI Agent with Langchain:  thoughts-to-post-ai-agent -> Consume user message by reading kafka topic, Connects with Ollama LLM Model, enrich the content, generate the image and publish enriched message back to kafka topic.
