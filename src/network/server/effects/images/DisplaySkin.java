package network.server.effects.images;

import network.server.util.FileHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

public class DisplaySkin extends DisplayImage {
    public DisplaySkin(ImageID id, UUID uuid, Color color) {
        super(id);
        loadImage(uuid, color);
    }

    private void loadImage(UUID uuid, Color color) {
        String skinUrl = "https://crafatar.com/renders/body/" + uuid + "?scale=10";
        setUrl(uuid.toString());
        String path = getUrl();

        // Download as a file so other servers on the same box can access it without an additional API call
        FileHandler.downloadImage(skinUrl, path);

        try {
            File file = new File(path);
            BufferedImage image = ImageIO.read(file);

            image = resizeImage(image);
            image = removeTransparency(image, color);

            ImageIO.write(image, "png", file);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private BufferedImage resizeImage(Image originalImage) {
        int originalHeight = originalImage.getHeight(null);
        int originalWidth = originalImage.getWidth(null);

        int size = 128;
        int height = size * 4;
        int width = size * 3;

        BufferedImage scaledBI = new BufferedImage(width, height, TYPE_INT_ARGB);
        Graphics2D g = scaledBI.createGraphics();
        g.drawImage(originalImage, (width - originalWidth) / 2, (height - originalHeight) / 2, originalWidth, originalHeight, null);
        g.dispose();
        return scaledBI;
    }

    private BufferedImage removeTransparency(BufferedImage image, Color color) {
        for(int y = 0; y < image.getHeight(); ++y) {
            for(int x = 0; x < image.getWidth(); ++x) {
                int argb = image.getRGB(x, y);

                if(color != null && ((argb >> 24) & 0xff) == 0) {
                    image.setRGB(x, y, color.getRGB());
                } else {
                    image.setRGB(x, y, argb);
                }
            }
        }

        return image;
    }
}
