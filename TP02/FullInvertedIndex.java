import java.io.*;
import java.util.*;

public class FullInvertedIndex {
    private RandomAccessFile indexFile;
    private Map<String, List<Integer>> invertedIndex;
    private String fileName;

    // Construtor
    public FullInvertedIndex(String fileName) throws IOException {
        this.fileName = fileName;
        this.invertedIndex = new HashMap<>();
        this.indexFile = new RandomAccessFile(fileName, "rw");
        loadIndex();
    }

    // Carrega o índice do arquivo para a memória
    private void loadIndex() throws IOException {
        if (indexFile.length() == 0) return;
        indexFile.seek(0);

        while (indexFile.getFilePointer() < indexFile.length()) {
            int recordSize = indexFile.readInt();
            byte[] recordData = new byte[recordSize];
            indexFile.read(recordData);
            ByteArrayInputStream bis = new ByteArrayInputStream(recordData);
            DataInputStream dis = new DataInputStream(bis);

            String key = dis.readUTF();
            int listSize = dis.readInt();
            List<Integer> idList = new ArrayList<>();
            for (int i = 0; i < listSize; i++) {
                idList.add(dis.readInt());
            }
            invertedIndex.put(key, idList);
        }
    }

    // Salva o índice da memória para o arquivo
    private void saveIndex() throws IOException {
        indexFile.setLength(0);
        indexFile.seek(0);

        for (Map.Entry<String, List<Integer>> entry : invertedIndex.entrySet()) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);

            dos.writeUTF(entry.getKey());
            dos.writeInt(entry.getValue().size());
            for (int id : entry.getValue()) {
                dos.writeInt(id);
            }

            byte[] recordData = bos.toByteArray();
            indexFile.writeInt(recordData.length);
            indexFile.write(recordData);
        }
    }

    // Adiciona palavras ao índice
    public void add(Hero hero) throws IOException {
        int id = hero.getId();
        // Obter palavras dos atributos relevantes
        List<String> words = extractKeywords(hero);

        for (String word : words) {
            List<Integer> idList = invertedIndex.getOrDefault(word, new ArrayList<>());
            if (!idList.contains(id)) {
                idList.add(id);
                invertedIndex.put(word, idList);
            }
        }
        saveIndex();
    }

    // Remove um herói do índice
    public void remove(Hero hero) throws IOException {
        int id = hero.getId();
        List<String> words = extractKeywords(hero);

        for (String word : words) {
            List<Integer> idList = invertedIndex.get(word);
            if (idList != null) {
                idList.remove(Integer.valueOf(id));
                if (idList.isEmpty()) {
                    invertedIndex.remove(word);
                } else {
                    invertedIndex.put(word, idList);
                }
            }
        }
        saveIndex();
    }

    // Atualiza o índice para um herói (após atualização)
    public void update(Hero oldHero, Hero newHero) throws IOException {
        remove(oldHero);
        add(newHero);
    }

    // Busca IDs por palavra-chave
    public List<Integer> search(String keyword) {
        return invertedIndex.getOrDefault(keyword, new ArrayList<>());
    }

    // Extrai palavras-chave dos atributos do herói
    private List<String> extractKeywords(Hero hero) {
        List<String> words = new ArrayList<>();

        // Adicionar palavras dos atributos (convertidos para lowercase)
        words.addAll(Arrays.asList(hero.getName().toLowerCase().split("\\s+")));
        words.add(hero.getUniverse().toLowerCase());
        words.add(hero.getAlignment().toLowerCase());
        words.add(hero.getIdentity().toLowerCase());
        words.add(hero.getGender().toLowerCase());
        words.add(hero.getStatus().toLowerCase());
        words.add(hero.getEyeColor().toLowerCase());
        words.add(hero.getHairColor().toLowerCase());
        // Podemos adicionar outros atributos se desejado

        // Remover possíveis caracteres especiais
        words.replaceAll(word -> word.replaceAll("[^a-z0-9]", ""));

        return words;
    }

    // Fecha o arquivo
    public void close() throws IOException {
        indexFile.close();
    }
}